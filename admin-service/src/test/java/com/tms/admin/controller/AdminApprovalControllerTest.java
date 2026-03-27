package com.tms.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.admin.dto.ApprovalActionRequest;
import com.tms.admin.dto.ApprovalTaskResponse;
import com.tms.admin.entity.ApprovalStatus;
import com.tms.admin.entity.TargetType;
import com.tms.admin.service.ApprovalService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminApprovalControllerTest {

    @Mock
    private ApprovalService approvalService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminApprovalController(approvalService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("GET /pending should return pending approvals")
    void getPendingApprovals_Success() throws Exception {
        ApprovalTaskResponse task = buildTaskResponse();
        when(approvalService.getPendingApprovals("MGR-001")).thenReturn(List.of(task));

        mockMvc.perform(get("/api/v1/admin/approvals/pending")
                        .header("X-User-Id", "MGR-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("APP-001"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /approve should approve a task")
    void approveTask_Success() throws Exception {
        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setComments("Approved");

        ApprovalTaskResponse response = buildTaskResponse();
        response.setStatus(ApprovalStatus.APPROVED);
        response.setComments("Approved");

        when(approvalService.approveTask(anyString(), anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/approvals/APP-001/approve")
                        .header("X-User-Id", "MGR-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.comments").value("Approved"));
    }

    @Test
    @DisplayName("POST /reject should reject a task")
    void rejectTask_Success() throws Exception {
        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setComments("Insufficient details");

        ApprovalTaskResponse response = buildTaskResponse();
        response.setStatus(ApprovalStatus.REJECTED);
        response.setComments("Insufficient details");

        when(approvalService.rejectTask(anyString(), anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/approvals/APP-001/reject")
                        .header("X-User-Id", "MGR-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.comments").value("Insufficient details"));
    }

    private ApprovalTaskResponse buildTaskResponse() {
        ApprovalTaskResponse response = new ApprovalTaskResponse();
        response.setId("APP-001");
        response.setTargetType(TargetType.LEAVE);
        response.setTargetId("LEAVE-001");
        response.setEmployeeId("EMP-001");
        response.setApproverId("MGR-001");
        response.setStatus(ApprovalStatus.PENDING);
        return response;
    }
}
