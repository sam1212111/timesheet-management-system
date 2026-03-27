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
class AdminReportingControllerTest {

    @Mock
    private ReportingService reportingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminReportingController(reportingService)).build();
    }

    @Test
    @DisplayName("GET /utilization should return system utilization report")
    void getUtilization_Success() throws Exception {
        when(reportingService.getSystemUtilization()).thenReturn(Map.of(
                "activeEmployees", 25,
                "averageWeeklyHours", 38.5
        ));

        mockMvc.perform(get("/api/v1/admin/reports/utilization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeEmployees").value(25))
                .andExpect(jsonPath("$.averageWeeklyHours").value(38.5));
    }
}
