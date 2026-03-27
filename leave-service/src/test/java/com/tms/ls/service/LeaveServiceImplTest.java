package com.tms.ls.service;

import com.tms.common.exception.ResourceNotFoundException;
import com.tms.ls.client.AuthServiceClient;
import com.tms.ls.dto.HolidayResponse;
import com.tms.ls.dto.LeaveRequestDto;
import com.tms.ls.dto.LeaveRequestedEvent;
import com.tms.ls.dto.LeaveResponse;
import com.tms.ls.dto.TeamCalendarResponse;
import com.tms.ls.entity.HolidayType;
import com.tms.ls.entity.LeaveBalance;
import com.tms.ls.entity.LeavePolicy;
import com.tms.ls.entity.LeaveRequest;
import com.tms.ls.entity.LeaveStatus;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.repository.LeaveBalanceRepository;
import com.tms.ls.repository.LeavePolicyRepository;
import com.tms.ls.repository.LeaveRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceImplTest {

    @Mock
    private LeaveBalanceRepository balanceRepository;

    @Mock
    private LeavePolicyRepository leavePolicyRepository;

    @Mock
    private LeaveRequestRepository requestRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private LeaveRequestEventPublisher leaveRequestEventPublisher;

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private LeaveServiceImpl service;

    @Test
    @DisplayName("requestLeave should save a submitted request, update pending balance, and publish an event")
    void requestLeave_SavesRequestUpdatesBalanceAndPublishesEvent() {
        LeaveRequestDto requestDto = new LeaveRequestDto();
        requestDto.setLeaveType(LeaveType.CASUAL);
        requestDto.setStartDate(LocalDate.of(2026, 3, 30));
        requestDto.setEndDate(LocalDate.of(2026, 4, 1));
        requestDto.setReason("Family event");

        LeaveBalance balance = new LeaveBalance("EMP-1", LeaveType.CASUAL, new BigDecimal("12"));
        LeaveRequest persisted = new LeaveRequest();
        persisted.setId("REQ-1");
        persisted.setEmployeeId("EMP-1");
        persisted.setLeaveType(LeaveType.CASUAL);
        persisted.setStartDate(requestDto.getStartDate());
        persisted.setEndDate(requestDto.getEndDate());
        persisted.setReason(requestDto.getReason());
        persisted.setStatus(LeaveStatus.SUBMITTED);
        persisted.setApproverId("MGR-1");
        persisted.setCreatedAt(LocalDateTime.of(2026, 3, 26, 10, 0));
        persisted.setUpdatedAt(LocalDateTime.of(2026, 3, 26, 10, 0));

        when(requestRepository.findOverlappingRequests("EMP-1", requestDto.getStartDate(), requestDto.getEndDate()))
                .thenReturn(List.of());
        when(balanceRepository.findByEmployeeIdAndLeaveType("EMP-1", LeaveType.CASUAL))
                .thenReturn(Optional.of(balance));
        when(authServiceClient.getManagerIdForEmployee("EMP-1", "Bearer token"))
                .thenReturn("MGR-1");
        when(requestRepository.save(any(LeaveRequest.class))).thenReturn(persisted);

        LeaveResponse response = service.requestLeave(requestDto, "EMP-1", "Bearer token");

        assertEquals("REQ-1", response.getId());
        assertEquals(LeaveStatus.SUBMITTED, response.getStatus());
        assertEquals(new BigDecimal("3"), balance.getPending());

        ArgumentCaptor<LeaveRequestedEvent> eventCaptor = ArgumentCaptor.forClass(LeaveRequestedEvent.class);
        verify(leaveRequestEventPublisher).publishLeaveRequestedEvent(eventCaptor.capture());
        assertEquals("REQ-1", eventCaptor.getValue().getRequestId());
        assertEquals("MGR-1", eventCaptor.getValue().getApproverId());
    }

    @Test
    @DisplayName("requestLeave should reject requests when the employee lacks sufficient balance")
    void requestLeave_RejectsWhenBalanceIsInsufficient() {
        LeaveRequestDto requestDto = new LeaveRequestDto();
        requestDto.setLeaveType(LeaveType.CASUAL);
        requestDto.setStartDate(LocalDate.of(2026, 3, 30));
        requestDto.setEndDate(LocalDate.of(2026, 4, 3));

        LeaveBalance balance = new LeaveBalance("EMP-1", LeaveType.CASUAL, new BigDecimal("2"));

        when(requestRepository.findOverlappingRequests("EMP-1", requestDto.getStartDate(), requestDto.getEndDate()))
                .thenReturn(List.of());
        when(balanceRepository.findByEmployeeIdAndLeaveType("EMP-1", LeaveType.CASUAL))
                .thenReturn(Optional.of(balance));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.requestLeave(requestDto, "EMP-1", "Bearer token"));

        assertTrue(exception.getMessage().contains("Insufficient leave balance"));
    }

    @Test
    @DisplayName("approveLeave should append approver comments and move pending days to used days")
    void approveLeave_UpdatesRequestAndBalance() {
        LeaveRequest request = new LeaveRequest();
        request.setId("REQ-2");
        request.setEmployeeId("EMP-2");
        request.setLeaveType(LeaveType.CASUAL);
        request.setStartDate(LocalDate.of(2026, 3, 30));
        request.setEndDate(LocalDate.of(2026, 3, 31));
        request.setReason("Vacation");
        request.setStatus(LeaveStatus.SUBMITTED);

        LeaveBalance balance = new LeaveBalance("EMP-2", LeaveType.CASUAL, new BigDecimal("10"));
        balance.setPending(new BigDecimal("2"));
        balance.setUsed(BigDecimal.ONE);

        when(requestRepository.findById("REQ-2")).thenReturn(Optional.of(request));
        when(balanceRepository.findByEmployeeIdAndLeaveType("EMP-2", LeaveType.CASUAL))
                .thenReturn(Optional.of(balance));

        service.approveLeave("REQ-2", Map.of("comments", "Approved quickly"));

        assertEquals(LeaveStatus.APPROVED, request.getStatus());
        assertTrue(request.getReason().contains("Approved quickly"));
        assertEquals(BigDecimal.ZERO, balance.getPending());
        assertEquals(new BigDecimal("3"), balance.getUsed());
    }

    @Test
    @DisplayName("rejectLeave should mark the request rejected and subtract pending days")
    void rejectLeave_UpdatesRequestAndPendingBalance() {
        LeaveRequest request = new LeaveRequest();
        request.setId("REQ-3");
        request.setEmployeeId("EMP-3");
        request.setLeaveType(LeaveType.CASUAL);
        request.setStartDate(LocalDate.of(2026, 3, 30));
        request.setEndDate(LocalDate.of(2026, 3, 31));
        request.setReason("Travel");
        request.setStatus(LeaveStatus.SUBMITTED);

        LeaveBalance balance = new LeaveBalance("EMP-3", LeaveType.CASUAL, new BigDecimal("10"));
        balance.setPending(new BigDecimal("2"));

        when(requestRepository.findById("REQ-3")).thenReturn(Optional.of(request));
        when(balanceRepository.findByEmployeeIdAndLeaveType("EMP-3", LeaveType.CASUAL))
                .thenReturn(Optional.of(balance));

        service.rejectLeave("REQ-3", Map.of("comments", "Project deadline"));

        assertEquals(LeaveStatus.REJECTED, request.getStatus());
        assertTrue(request.getReason().contains("Project deadline"));
        assertEquals(BigDecimal.ZERO, balance.getPending());
    }

    @Test
    @DisplayName("cancelLeaveRequest should subtract pending balance for submitted requests")
    void cancelLeaveRequest_SubmittedRequestSubtractsPendingDays() {
        LeaveRequest request = new LeaveRequest();
        request.setId("REQ-4");
        request.setEmployeeId("EMP-4");
        request.setLeaveType(LeaveType.CASUAL);
        request.setStartDate(LocalDate.of(2026, 3, 30));
        request.setEndDate(LocalDate.of(2026, 3, 31));
        request.setStatus(LeaveStatus.SUBMITTED);

        LeaveBalance balance = new LeaveBalance("EMP-4", LeaveType.CASUAL, new BigDecimal("10"));
        balance.setPending(new BigDecimal("2"));

        when(requestRepository.findById("REQ-4")).thenReturn(Optional.of(request));
        when(balanceRepository.findByEmployeeIdAndLeaveType("EMP-4", LeaveType.CASUAL))
                .thenReturn(Optional.of(balance));

        service.cancelLeaveRequest("REQ-4", "EMP-4");

        assertEquals(LeaveStatus.CANCELLED, request.getStatus());
        assertEquals(BigDecimal.ZERO, balance.getPending());
        assertEquals(BigDecimal.ZERO, balance.getUsed());
    }

    @Test
    @DisplayName("cancelLeaveRequest should subtract used balance for approved requests")
    void cancelLeaveRequest_ApprovedRequestSubtractsUsedDays() {
        LeaveRequest request = new LeaveRequest();
        request.setId("REQ-4A");
        request.setEmployeeId("EMP-4A");
        request.setLeaveType(LeaveType.CASUAL);
        request.setStartDate(LocalDate.of(2026, 3, 30));
        request.setEndDate(LocalDate.of(2026, 3, 31));
        request.setStatus(LeaveStatus.APPROVED);

        LeaveBalance balance = new LeaveBalance("EMP-4A", LeaveType.CASUAL, new BigDecimal("10"));
        balance.setUsed(new BigDecimal("2"));

        when(requestRepository.findById("REQ-4A")).thenReturn(Optional.of(request));
        when(balanceRepository.findByEmployeeIdAndLeaveType("EMP-4A", LeaveType.CASUAL))
                .thenReturn(Optional.of(balance));

        service.cancelLeaveRequest("REQ-4A", "EMP-4A");

        assertEquals(LeaveStatus.CANCELLED, request.getStatus());
        assertEquals(BigDecimal.ZERO, balance.getUsed());
    }

    @Test
    @DisplayName("cancelLeaveRequest should reject cancellation attempts by another employee")
    void cancelLeaveRequest_RejectsForeignEmployee() {
        LeaveRequest request = new LeaveRequest();
        request.setId("REQ-5");
        request.setEmployeeId("EMP-5");
        request.setStatus(LeaveStatus.SUBMITTED);

        when(requestRepository.findById("REQ-5")).thenReturn(Optional.of(request));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.cancelLeaveRequest("REQ-5", "EMP-OTHER"));

        assertEquals("Cannot cancel someone else's leave request", exception.getMessage());
        verify(balanceRepository, never()).findByEmployeeIdAndLeaveType(any(), any());
    }

    @Test
    @DisplayName("requestLeave should reject overlapping requests")
    void requestLeave_RejectsOverlappingRequests() {
        LeaveRequestDto requestDto = new LeaveRequestDto();
        requestDto.setLeaveType(LeaveType.CASUAL);
        requestDto.setStartDate(LocalDate.of(2026, 4, 1));
        requestDto.setEndDate(LocalDate.of(2026, 4, 2));

        when(requestRepository.findOverlappingRequests("EMP-OVERLAP", requestDto.getStartDate(), requestDto.getEndDate()))
                .thenReturn(List.of(new LeaveRequest()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.requestLeave(requestDto, "EMP-OVERLAP", "Bearer token"));

        assertEquals("You already have an active leave request during this period", exception.getMessage());
    }

    @Test
    @DisplayName("requestLeave should reject requests when no manager is assigned")
    void requestLeave_RejectsWhenNoManagerAssigned() {
        LeaveRequestDto requestDto = new LeaveRequestDto();
        requestDto.setLeaveType(LeaveType.CASUAL);
        requestDto.setStartDate(LocalDate.of(2026, 3, 30));
        requestDto.setEndDate(LocalDate.of(2026, 4, 1));

        LeaveBalance balance = new LeaveBalance("EMP-NOMGR", LeaveType.CASUAL, new BigDecimal("12"));

        when(requestRepository.findOverlappingRequests("EMP-NOMGR", requestDto.getStartDate(), requestDto.getEndDate()))
                .thenReturn(List.of());
        when(balanceRepository.findByEmployeeIdAndLeaveType("EMP-NOMGR", LeaveType.CASUAL))
                .thenReturn(Optional.of(balance));
        when(authServiceClient.getManagerIdForEmployee("EMP-NOMGR", "Bearer token"))
                .thenReturn(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.requestLeave(requestDto, "EMP-NOMGR", "Bearer token"));

        assertEquals("Cannot submit leave: No manager assigned to approve", exception.getMessage());
    }

    @Test
    @DisplayName("initializeBalances should create balances from active policies when none exist")
    void initializeBalances_SavesBalancesFromPolicies() {
        LeavePolicy casual = new LeavePolicy();
        casual.setLeaveType(LeaveType.CASUAL);
        casual.setDaysAllowed(new BigDecimal("12"));

        LeavePolicy sick = new LeavePolicy();
        sick.setLeaveType(LeaveType.SICK);
        sick.setDaysAllowed(new BigDecimal("8"));

        when(balanceRepository.findByEmployeeId("EMP-6")).thenReturn(List.of());
        when(leavePolicyRepository.findByActiveTrue()).thenReturn(List.of(casual, sick));

        service.initializeBalances("EMP-6");

        ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(balanceRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(List.of(LeaveType.CASUAL, LeaveType.SICK),
                captor.getAllValues().stream().map(LeaveBalance::getLeaveType).toList());
    }

    @Test
    @DisplayName("getTeamCalendar should map requests and include holidays")
    void getTeamCalendar_ReturnsMappedTeamCalendar() {
        LeaveRequest request = new LeaveRequest();
        request.setId("REQ-7");
        request.setEmployeeId("EMP-7");
        request.setLeaveType(LeaveType.CASUAL);
        request.setStartDate(LocalDate.of(2026, 4, 7));
        request.setEndDate(LocalDate.of(2026, 4, 8));
        request.setStatus(LeaveStatus.SUBMITTED);
        request.setReason("Medical");
        request.setApproverId("MGR-7");

        HolidayResponse holiday = new HolidayResponse();
        holiday.setId("HOL-7");
        holiday.setDate(LocalDate.of(2026, 4, 10));
        holiday.setName("Festival");
        holiday.setType(HolidayType.MANDATORY);

        when(requestRepository.findByApproverId("MGR-7")).thenReturn(List.of(request));
        when(holidayService.getAllHolidays()).thenReturn(List.of(holiday));

        TeamCalendarResponse response = service.getTeamCalendar("MGR-7");

        assertEquals(1, response.getTeamLeaves().size());
        assertEquals(1, response.getHolidays().size());
        assertEquals("REQ-7", response.getTeamLeaves().get(0).getId());
    }

    @Test
    @DisplayName("getBalances should map stored balances")
    void getBalances_ReturnsMappedBalances() {
        LeaveBalance balance = new LeaveBalance("EMP-8", LeaveType.SICK, new BigDecimal("9"));
        balance.setId("BAL-8");
        balance.setUsed(new BigDecimal("2"));
        balance.setPending(BigDecimal.ONE);

        when(balanceRepository.findByEmployeeId("EMP-8")).thenReturn(List.of(balance));

        var result = service.getBalances("EMP-8");

        assertEquals(1, result.size());
        assertEquals("BAL-8", result.get(0).getId());
        assertEquals(LeaveType.SICK, result.get(0).getLeaveType());
        assertEquals(new BigDecimal("2"), result.get(0).getUsed());
    }

    @Test
    @DisplayName("getMyRequests should map leave requests in descending order")
    void getMyRequests_ReturnsMappedRequests() {
        LeaveRequest request = new LeaveRequest();
        request.setId("REQ-8");
        request.setEmployeeId("EMP-8");
        request.setLeaveType(LeaveType.SICK);
        request.setStartDate(LocalDate.of(2026, 4, 2));
        request.setEndDate(LocalDate.of(2026, 4, 3));
        request.setStatus(LeaveStatus.APPROVED);
        request.setReason("Recovery");

        when(requestRepository.findByEmployeeIdOrderByStartDateDesc("EMP-8")).thenReturn(List.of(request));

        var result = service.getMyRequests("EMP-8");

        assertEquals(1, result.size());
        assertEquals("REQ-8", result.get(0).getId());
        assertEquals(LeaveStatus.APPROVED, result.get(0).getStatus());
    }

    @Test
    @DisplayName("initializeBalances should reject initialization when no active policies exist")
    void initializeBalances_ThrowsWhenNoPoliciesExist() {
        when(balanceRepository.findByEmployeeId("EMP-9")).thenReturn(List.of());
        when(leavePolicyRepository.findByActiveTrue()).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.initializeBalances("EMP-9"));

        assertEquals("No active leave policies found to initialize balances", exception.getMessage());
    }

    @Test
    @DisplayName("requestLeave should reject inverted date ranges")
    void requestLeave_RejectsStartDateAfterEndDate() {
        LeaveRequestDto requestDto = new LeaveRequestDto();
        requestDto.setLeaveType(LeaveType.CASUAL);
        requestDto.setStartDate(LocalDate.of(2026, 4, 5));
        requestDto.setEndDate(LocalDate.of(2026, 4, 4));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.requestLeave(requestDto, "EMP-10", "Bearer token"));

        assertEquals("Start date cannot be after end date", exception.getMessage());
    }

    @Test
    @DisplayName("approveLeave should throw when the request cannot be found")
    void approveLeave_ThrowsWhenRequestMissing() {
        when(requestRepository.findById("REQ-404")).thenReturn(Optional.empty());

        Exception exception = assertThrows(ResourceNotFoundException.class,
                () -> service.approveLeave("REQ-404", null));

        assertInstanceOf(ResourceNotFoundException.class, exception);
    }
}
