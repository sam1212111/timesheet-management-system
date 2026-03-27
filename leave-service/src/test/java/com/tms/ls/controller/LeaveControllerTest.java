package com.tms.ls.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tms.ls.dto.LeaveBalanceResponse;
import com.tms.ls.dto.LeaveRequestDto;
import com.tms.ls.dto.LeaveResponse;
import com.tms.ls.dto.TeamCalendarResponse;
import com.tms.ls.entity.LeaveStatus;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.service.LeaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LeaveControllerTest {

    @Mock
    private LeaveService leaveService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(new LeaveController(leaveService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("GET /balances should return employee leave balances")
    void getMyBalances_Success() throws Exception {
        LeaveBalanceResponse balance = new LeaveBalanceResponse();
        balance.setId("LB-001");
        balance.setEmployeeId("EMP-001");
        balance.setLeaveType(LeaveType.CASUAL);
        balance.setTotalAllowed(new BigDecimal("12"));
        balance.setUsed(new BigDecimal("2"));
        balance.setPending(BigDecimal.ONE);

        when(leaveService.getBalances("EMP-001")).thenReturn(List.of(balance));

        mockMvc.perform(get("/api/v1/leave/balances")
                        .header("X-User-Id", "EMP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("LB-001"))
                .andExpect(jsonPath("$[0].leaveType").value("CASUAL"));
    }

    @Test
    @DisplayName("POST /requests should create a leave request")
    void requestLeave_Success() throws Exception {
        LeaveRequestDto request = new LeaveRequestDto();
        request.setLeaveType(LeaveType.CASUAL);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));
        request.setReason("Family event");

        LeaveResponse response = new LeaveResponse();
        response.setId("LR-001");
        response.setEmployeeId("EMP-001");
        response.setLeaveType(LeaveType.CASUAL);
        response.setStatus(LeaveStatus.SUBMITTED);

        when(leaveService.requestLeave(any(LeaveRequestDto.class), anyString(), anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/leave/requests")
                        .header("X-User-Id", "EMP-001")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("LR-001"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("GET /requests should return leave history")
    void getMyRequests_Success() throws Exception {
        LeaveResponse response = new LeaveResponse();
        response.setId("LR-002");
        response.setEmployeeId("EMP-001");
        response.setLeaveType(LeaveType.SICK);
        response.setStatus(LeaveStatus.APPROVED);

        when(leaveService.getMyRequests("EMP-001")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/leave/requests")
                        .header("X-User-Id", "EMP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("LR-002"))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /balances/initialize should delegate balance initialization")
    void initializeBalances_Success() throws Exception {
        mockMvc.perform(post("/api/v1/leave/balances/initialize")
                        .param("employeeId", "EMP-001"))
                .andExpect(status().isOk());

        verify(leaveService).initializeBalances("EMP-001");
    }

    @Test
    @DisplayName("GET /team-calendar should return the manager team calendar")
    void getTeamCalendar_Success() throws Exception {
        TeamCalendarResponse response = new TeamCalendarResponse(List.of(), List.of());

        when(leaveService.getTeamCalendar("MGR-001")).thenReturn(response);

        mockMvc.perform(get("/api/v1/leave/team-calendar")
                        .header("X-User-Id", "MGR-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamLeaves").isArray())
                .andExpect(jsonPath("$.holidays").isArray());
    }

    @Test
    @DisplayName("PATCH /requests/{id}/approve should delegate approval")
    void approveLeave_Success() throws Exception {
        mockMvc.perform(patch("/api/v1/leave/requests/LR-010/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comments", "Looks good"))))
                .andExpect(status().isOk());

        verify(leaveService).approveLeave(anyString(), anyMap());
    }

    @Test
    @DisplayName("PATCH /requests/{id}/reject should delegate rejection")
    void rejectLeave_Success() throws Exception {
        mockMvc.perform(patch("/api/v1/leave/requests/LR-010/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comments", "Need coverage"))))
                .andExpect(status().isOk());

        verify(leaveService).rejectLeave(anyString(), anyMap());
    }

    @Test
    @DisplayName("PATCH /requests/{id}/cancel should cancel the leave request")
    void cancelLeaveRequest_Success() throws Exception {
        mockMvc.perform(patch("/api/v1/leave/requests/LR-011/cancel")
                        .header("X-User-Id", "EMP-001"))
                .andExpect(status().isOk());

        verify(leaveService).cancelLeaveRequest("LR-011", "EMP-001");
    }
}
