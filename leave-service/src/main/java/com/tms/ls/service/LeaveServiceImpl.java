package com.tms.ls.service;

import com.tms.common.exception.ResourceNotFoundException;
import com.tms.ls.client.AuthServiceClient;
import com.tms.ls.dto.LeaveBalanceResponse;
import com.tms.ls.dto.LeaveRequestDto;
import com.tms.ls.dto.LeaveRequestedEvent;
import com.tms.ls.dto.LeaveResponse;
import com.tms.ls.dto.TeamCalendarResponse;
import com.tms.ls.entity.LeaveBalance;
import com.tms.ls.entity.LeavePolicy;
import com.tms.ls.entity.LeaveRequest;
import com.tms.ls.entity.LeaveStatus;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.repository.LeaveBalanceRepository;
import com.tms.ls.repository.LeavePolicyRepository;
import com.tms.ls.repository.LeaveRequestRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveServiceImpl implements LeaveService {
    private static final String COMMENTS_KEY = "comments";
    private static final String LEAVE_REQUEST_NOT_FOUND = "Leave request not found";
    private static final String LEAVE_BALANCE_NOT_FOUND = "Leave balance not found";

    private final LeaveBalanceRepository balanceRepository;
    private final LeavePolicyRepository leavePolicyRepository;
    private final LeaveRequestRepository requestRepository;
    private final AuthServiceClient authServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final HolidayService holidayService;

    public LeaveServiceImpl(LeaveBalanceRepository balanceRepository,
                            LeavePolicyRepository leavePolicyRepository,
                            LeaveRequestRepository requestRepository,
                            AuthServiceClient authServiceClient,
                            RabbitTemplate rabbitTemplate,
                            HolidayService holidayService) {
        this.balanceRepository = balanceRepository;
        this.leavePolicyRepository = leavePolicyRepository;
        this.requestRepository = requestRepository;
        this.authServiceClient = authServiceClient;
        this.rabbitTemplate = rabbitTemplate;
        this.holidayService = holidayService;
    }

    @Override
    @Transactional(readOnly = true)
    public TeamCalendarResponse getTeamCalendar(String managerId) {
        List<LeaveResponse> teamLeaves = requestRepository.findByApproverId(managerId)
                .stream().map(this::mapReqToResponse).toList();
        return new TeamCalendarResponse(teamLeaves, holidayService.getAllHolidays());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getBalances(String employeeId) {
        List<LeaveBalance> balances = balanceRepository.findByEmployeeId(employeeId);
        if (balances.isEmpty()) {
            return List.of();
        }
        return balances.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public void initializeBalances(String employeeId) {
        List<LeaveBalance> existing = balanceRepository.findByEmployeeId(employeeId);
        if (existing.isEmpty()) {
            List<LeavePolicy> activePolicies = leavePolicyRepository.findByActiveTrue();
            if (activePolicies.isEmpty()) {
                throw new IllegalArgumentException("No active leave policies found to initialize balances");
            }

            activePolicies.forEach(policy ->
                    balanceRepository.save(new LeaveBalance(
                            employeeId,
                            policy.getLeaveType(),
                            policy.getDaysAllowed()
                    ))
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> getMyRequests(String employeeId) {
        return requestRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)
                .stream().map(this::mapReqToResponse).toList();
    }

    @Override
    @Transactional
    public LeaveResponse requestLeave(LeaveRequestDto requestDto, String employeeId, String authorization) {
        if (requestDto.getStartDate().isAfter(requestDto.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        List<LeaveRequest> overlaps = requestRepository.findOverlappingRequests(employeeId, requestDto.getStartDate(), requestDto.getEndDate());
        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException("You already have an active leave request during this period");
        }

        BigDecimal requestedDays = calculateWorkingDays(requestDto.getStartDate(), requestDto.getEndDate());
        if (requestedDays.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Requested period does not contain any working days");
        }

        LeaveBalance balance = balanceRepository.findByEmployeeIdAndLeaveType(employeeId, requestDto.getLeaveType())
                .orElseThrow(() -> new IllegalArgumentException(LEAVE_BALANCE_NOT_FOUND + " for type: " + requestDto.getLeaveType()));

        BigDecimal available = balance.getTotalAllowed().subtract(balance.getUsed()).subtract(balance.getPending());
        
        if (available.compareTo(requestedDays) < 0 && requestDto.getLeaveType() != LeaveType.UNPAID) {
            throw new IllegalArgumentException("Insufficient leave balance. requested: " + requestedDays + ", available: " + available);
        }

        String approverId = authServiceClient.getManagerIdForEmployee(employeeId, authorization);
        if (approverId == null || approverId.isBlank()) {
            throw new IllegalArgumentException("Cannot submit leave: No manager assigned to approve");
        }

        LeaveRequest request = new LeaveRequest();
        request.setEmployeeId(employeeId);
        request.setLeaveType(requestDto.getLeaveType());
        request.setStartDate(requestDto.getStartDate());
        request.setEndDate(requestDto.getEndDate());
        request.setReason(requestDto.getReason());
        request.setStatus(LeaveStatus.SUBMITTED);
        request.setApproverId(approverId);
        
        request = requestRepository.save(request);

        balance.setPending(balance.getPending().add(requestedDays));
        balanceRepository.save(balance);

        LeaveRequestedEvent event = new LeaveRequestedEvent(
                request.getId(),
                employeeId,
                approverId,
                request.getLeaveType().name(),
                request.getStartDate(),
                request.getEndDate()
        );
        rabbitTemplate.convertAndSend(
                com.tms.ls.config.RabbitMQConfig.EXCHANGE,
                com.tms.ls.config.RabbitMQConfig.LEAVE_ROUTING_KEY,
                event);

        return mapReqToResponse(request);
    }

    @Override
    @Transactional
    public void approveLeave(String id, java.util.Map<String, String> comments) {
        LeaveRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LEAVE_REQUEST_NOT_FOUND));
        request.setStatus(LeaveStatus.APPROVED);
        if (comments != null && comments.containsKey(COMMENTS_KEY)) {
            request.setReason(request.getReason() + " [Approver Comment: " + comments.get(COMMENTS_KEY) + "]");
        }
        requestRepository.save(request);
        
        LeaveBalance balance = balanceRepository.findByEmployeeIdAndLeaveType(request.getEmployeeId(), request.getLeaveType())
                .orElseThrow(() -> new IllegalArgumentException(LEAVE_BALANCE_NOT_FOUND));
        BigDecimal requestedDays = calculateWorkingDays(request.getStartDate(), request.getEndDate());
        balance.setPending(balance.getPending().subtract(requestedDays));
        balance.setUsed(balance.getUsed().add(requestedDays));
        balanceRepository.save(balance);
    }

    @Override
    @Transactional
    public void rejectLeave(String id, java.util.Map<String, String> comments) {
        LeaveRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LEAVE_REQUEST_NOT_FOUND));
        request.setStatus(LeaveStatus.REJECTED);
        if (comments != null && comments.containsKey(COMMENTS_KEY)) {
            request.setReason(request.getReason() + " [Approver Comment: " + comments.get(COMMENTS_KEY) + "]");
        }
        requestRepository.save(request);
        
        LeaveBalance balance = balanceRepository.findByEmployeeIdAndLeaveType(request.getEmployeeId(), request.getLeaveType())
                .orElseThrow(() -> new IllegalArgumentException(LEAVE_BALANCE_NOT_FOUND));
        BigDecimal requestedDays = calculateWorkingDays(request.getStartDate(), request.getEndDate());
        balance.setPending(balance.getPending().subtract(requestedDays));
        balanceRepository.save(balance);
    }

    @Override
    @Transactional
    public void cancelLeaveRequest(String id, String employeeId) {
        LeaveRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LEAVE_REQUEST_NOT_FOUND));
        
        if (!request.getEmployeeId().equals(employeeId)) {
            throw new IllegalArgumentException("Cannot cancel someone else's leave request");
        }

        if (request.getStatus() == LeaveStatus.CANCELLED || request.getStatus() == LeaveStatus.REJECTED) {
            throw new IllegalArgumentException("Leave request is already " + request.getStatus());
        }

        LeaveStatus oldStatus = request.getStatus();
        request.setStatus(LeaveStatus.CANCELLED);
        requestRepository.save(request);

        LeaveBalance balance = balanceRepository.findByEmployeeIdAndLeaveType(employeeId, request.getLeaveType())
                .orElseThrow(() -> new IllegalArgumentException(LEAVE_BALANCE_NOT_FOUND));
        
        BigDecimal requestedDays = calculateWorkingDays(request.getStartDate(), request.getEndDate());
        
        if (oldStatus == LeaveStatus.SUBMITTED) {
            balance.setPending(balance.getPending().subtract(requestedDays));
        } else if (oldStatus == LeaveStatus.APPROVED) {
            balance.setUsed(balance.getUsed().subtract(requestedDays));
        }
        
        balanceRepository.save(balance);
    }

    private BigDecimal calculateWorkingDays(LocalDate start, LocalDate end) {
        long days = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days++;
            }
            current = current.plusDays(1);
        }
        return new BigDecimal(days);
    }

    private LeaveBalanceResponse mapToResponse(LeaveBalance entity) {
        LeaveBalanceResponse resp = new LeaveBalanceResponse();
        resp.setId(entity.getId());
        resp.setEmployeeId(entity.getEmployeeId());
        resp.setLeaveType(entity.getLeaveType());
        resp.setTotalAllowed(entity.getTotalAllowed());
        resp.setUsed(entity.getUsed());
        resp.setPending(entity.getPending());
        return resp;
    }

    private LeaveResponse mapReqToResponse(LeaveRequest entity) {
        LeaveResponse resp = new LeaveResponse();
        resp.setId(entity.getId());
        resp.setEmployeeId(entity.getEmployeeId());
        resp.setLeaveType(entity.getLeaveType());
        resp.setStartDate(entity.getStartDate());
        resp.setEndDate(entity.getEndDate());
        resp.setStatus(entity.getStatus());
        resp.setReason(entity.getReason());
        resp.setApproverId(entity.getApproverId());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
