package com.tms.admin.service;

import com.tms.admin.entity.ApprovalStatus;
import com.tms.admin.entity.TargetType;
import com.tms.admin.repository.ApprovalTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReportingServiceImpl implements ReportingService {

    private final ApprovalTaskRepository approvalTaskRepository;

    public ReportingServiceImpl(ApprovalTaskRepository approvalTaskRepository) {
        this.approvalTaskRepository = approvalTaskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemUtilization() {
        long totalTasks = approvalTaskRepository.count();
        long pendingTasks = approvalTaskRepository.countByStatus(ApprovalStatus.PENDING);
        long approvedTasks = approvalTaskRepository.countByStatus(ApprovalStatus.APPROVED);
        long rejectedTasks = approvalTaskRepository.countByStatus(ApprovalStatus.REJECTED);
        long leaveTasks = approvalTaskRepository.countByTargetType(TargetType.LEAVE);
        long timesheetTasks = approvalTaskRepository.countByTargetType(TargetType.TIMESHEET);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalApprovalTasks", totalTasks);
        metrics.put("pendingApprovalTasks", pendingTasks);
        metrics.put("approvedApprovalTasks", approvedTasks);
        metrics.put("rejectedApprovalTasks", rejectedTasks);
        metrics.put("leaveApprovalTasks", leaveTasks);
        metrics.put("timesheetApprovalTasks", timesheetTasks);
        metrics.put("approvalCompletionRate", calculatePercentage(approvedTasks + rejectedTasks, totalTasks));

        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getComplianceDashboard() {
        long totalTasks = approvalTaskRepository.count();
        long pendingTasks = approvalTaskRepository.countByStatus(ApprovalStatus.PENDING);
        long approvedTasks = approvalTaskRepository.countByStatus(ApprovalStatus.APPROVED);
        long rejectedTasks = approvalTaskRepository.countByStatus(ApprovalStatus.REJECTED);
        long pendingLeaveApprovals = approvalTaskRepository.countByTargetTypeAndStatus(TargetType.LEAVE, ApprovalStatus.PENDING);
        long pendingTimesheetApprovals = approvalTaskRepository.countByTargetTypeAndStatus(TargetType.TIMESHEET, ApprovalStatus.PENDING);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("pendingApprovalsSystemWide", pendingTasks);
        metrics.put("approvedApprovalsSystemWide", approvedTasks);
        metrics.put("rejectedApprovalsSystemWide", rejectedTasks);
        metrics.put("pendingLeaveApprovals", pendingLeaveApprovals);
        metrics.put("pendingTimesheetApprovals", pendingTimesheetApprovals);
        metrics.put("approvalCompletionRate", calculatePercentage(approvedTasks + rejectedTasks, totalTasks));
        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEmployeeSummaryDashboard(String employeeId) {
        long totalRequests = approvalTaskRepository.countByEmployeeId(employeeId);
        long pendingRequests = approvalTaskRepository.countByEmployeeIdAndStatus(employeeId, ApprovalStatus.PENDING);
        long approvedRequests = approvalTaskRepository.countByEmployeeIdAndStatus(employeeId, ApprovalStatus.APPROVED);
        long rejectedRequests = approvalTaskRepository.countByEmployeeIdAndStatus(employeeId, ApprovalStatus.REJECTED);
        long leaveRequests = approvalTaskRepository.countByEmployeeIdAndTargetType(employeeId, TargetType.LEAVE);
        long timesheetRequests = approvalTaskRepository.countByEmployeeIdAndTargetType(employeeId, TargetType.TIMESHEET);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("myTotalRequests", totalRequests);
        metrics.put("myPendingRequests", pendingRequests);
        metrics.put("myApprovedRequests", approvedRequests);
        metrics.put("myRejectedRequests", rejectedRequests);
        metrics.put("myLeaveRequests", leaveRequests);
        metrics.put("myTimesheetRequests", timesheetRequests);
        return metrics;
    }

    private String calculatePercentage(long value, long total) {
        if (total == 0) {
            return "0.00%";
        }
        double percentage = (value * 100.0) / total;
        return String.format("%.2f%%", percentage);
    }
}
