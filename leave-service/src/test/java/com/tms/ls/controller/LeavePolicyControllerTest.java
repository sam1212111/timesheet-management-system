package com.tms.ls.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.ls.dto.LeavePolicyDto;
import com.tms.ls.entity.LeavePolicy;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.service.LeavePolicyService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LeavePolicyControllerTest {

    @Mock
    private LeavePolicyService leavePolicyService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new LeavePolicyController(leavePolicyService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("GET /policies should return all leave policies")
    void getAllPolicies_Success() throws Exception {
        LeavePolicy policy = buildPolicy();
        when(leavePolicyService.getAllPolicies()).thenReturn(List.of(policy));

        mockMvc.perform(get("/api/v1/leave/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("POL-001"))
                .andExpect(jsonPath("$[0].leaveType").value("CASUAL"));
    }

    @Test
    @DisplayName("GET /policies/{type} should return a single policy")
    void getPolicyByType_Success() throws Exception {
        LeavePolicy policy = buildPolicy();
        when(leavePolicyService.getPolicyByType(LeaveType.CASUAL)).thenReturn(policy);

        mockMvc.perform(get("/api/v1/leave/policies/CASUAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("POL-001"))
                .andExpect(jsonPath("$.daysAllowed").value(12));
    }

    @Test
    @DisplayName("POST /policies should create or update a policy")
    void createOrUpdatePolicy_Success() throws Exception {
        LeavePolicyDto request = new LeavePolicyDto();
        request.setLeaveType(LeaveType.CASUAL);
        request.setDaysAllowed(new BigDecimal("12"));
        request.setActive(true);

        when(leavePolicyService.createOrUpdatePolicy(any(LeavePolicyDto.class))).thenReturn(buildPolicy());

        mockMvc.perform(post("/api/v1/leave/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leaveType").value("CASUAL"));
    }

    @Test
    @DisplayName("DELETE /policies/{id} should delete a policy")
    void deletePolicy_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/leave/policies/POL-001"))
                .andExpect(status().isNoContent());

        verify(leavePolicyService).deletePolicy("POL-001");
    }

    private LeavePolicy buildPolicy() {
        LeavePolicy policy = new LeavePolicy();
        policy.setId("POL-001");
        policy.setLeaveType(LeaveType.CASUAL);
        policy.setDaysAllowed(new BigDecimal("12"));
        policy.setCarryForwardAllowed(true);
        policy.setActive(true);
        return policy;
    }
}
