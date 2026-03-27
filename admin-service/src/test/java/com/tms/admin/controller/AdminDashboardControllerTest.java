package com.tms.admin.controller;

import com.tms.admin.service.ReportingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock
    private ReportingService reportingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(reportingService)).build();
    }

    @Test
    @DisplayName("GET /compliance should return compliance metrics")
    void getComplianceDashboard_Success() throws Exception {
        when(reportingService.getComplianceDashboard()).thenReturn(Map.of(
                "pendingApprovals", 4,
                "submittedTimesheets", 12
        ));

        mockMvc.perform(get("/api/v1/admin/dashboard/compliance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingApprovals").value(4))
                .andExpect(jsonPath("$.submittedTimesheets").value(12));
    }

    @Test
    @DisplayName("GET /employee-summary should return employee dashboard data")
    void getEmployeeSummaryDashboard_Success() throws Exception {
        when(reportingService.getEmployeeSummaryDashboard("EMP-001")).thenReturn(Map.of(
                "leaveBalance", 8,
                "pendingTimesheets", 1
        ));

        mockMvc.perform(get("/api/v1/admin/dashboard/employee-summary")
                        .header("X-User-Id", "EMP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaveBalance").value(8))
                .andExpect(jsonPath("$.pendingTimesheets").value(1));
    }
}
