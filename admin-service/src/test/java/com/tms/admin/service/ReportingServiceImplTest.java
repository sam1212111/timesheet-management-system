package com.tms.admin.service;

import com.tms.admin.entity.ApprovalStatus;
import com.tms.admin.entity.TargetType;
import com.tms.admin.repository.ApprovalTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceImplTest {

    @Mock
    private ApprovalTaskRepository approvalTaskRepository;

    @InjectMocks
    private ReportingServiceImpl service;

    @Test
    @DisplayName("getSystemUtilization should aggregate repository metrics")
    void getSystemUtilization_ReturnsMetrics() {
        when(approvalTaskRepository.count()).thenReturn(10L);
        when(approvalTaskRepository.countByStatus(ApprovalStatus.PENDING)).thenReturn(2L);
        when(approvalTaskRepository.countByStatus(ApprovalStatus.APPROVED)).thenReturn(5L);
        when(approvalTaskRepository.countByStatus(ApprovalStatus.REJECTED)).thenReturn(3L);
        when(approvalTaskRepository.countByTargetType(TargetType.LEAVE)).thenReturn(6L);
        when(approvalTaskRepository.countByTargetType(TargetType.TIMESHEET)).thenReturn(4L);

        Map<String, Object> result = service.getSystemUtilization();

        assertEquals(10L, result.get("totalApprovalTasks"));
        assertEquals("80.00%", result.get("approvalCompletionRate"));
    }

    @Test
    @DisplayName("getEmployeeSummaryDashboard should return employee scoped metrics")
    void getEmployeeSummaryDashboard_ReturnsEmployeeMetrics() {
        when(approvalTaskRepository.countByEmployeeId("EMP-1")).thenReturn(7L);
        when(approvalTaskRepository.countByEmployeeIdAndStatus("EMP-1", ApprovalStatus.PENDING)).thenReturn(2L);
        when(approvalTaskRepository.countByEmployeeIdAndStatus("EMP-1", ApprovalStatus.APPROVED)).thenReturn(4L);
        when(approvalTaskRepository.countByEmployeeIdAndStatus("EMP-1", ApprovalStatus.REJECTED)).thenReturn(1L);
        when(approvalTaskRepository.countByEmployeeIdAndTargetType("EMP-1", TargetType.LEAVE)).thenReturn(5L);
        when(approvalTaskRepository.countByEmployeeIdAndTargetType("EMP-1", TargetType.TIMESHEET)).thenReturn(2L);

        Map<String, Object> result = service.getEmployeeSummaryDashboard("EMP-1");

        assertEquals(7L, result.get("myTotalRequests"));
        assertEquals(5L, result.get("myLeaveRequests"));
        assertEquals(2L, result.get("myTimesheetRequests"));
    }
}
