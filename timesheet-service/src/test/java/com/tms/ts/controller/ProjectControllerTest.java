package com.tms.ts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tms.ts.dto.ProjectRequest;
import com.tms.ts.dto.ProjectResponse;
import com.tms.ts.exception.GlobalExceptionHandler;
import com.tms.ts.service.ProjectService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ProjectResponse projectResponse;
    private ProjectRequest projectRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectController(projectService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        projectResponse = new ProjectResponse();
        projectResponse.setId("PRJ-101");
        projectResponse.setCode("TMS001");
        projectResponse.setName("TMS Dashboard");
        projectResponse.setActive(true);

        projectRequest = new ProjectRequest();
        projectRequest.setCode("TMS001");
        projectRequest.setName("TMS Dashboard");
    }

    @Test
    @DisplayName("POST /projects - Should create project")
    void createProject_Success() throws Exception {
        when(projectService.createProject(any(ProjectRequest.class))).thenReturn(projectResponse);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("PRJ-101"))
                .andExpect(jsonPath("$.name").value("TMS Dashboard"));
    }

    @Test
    @DisplayName("PUT /projects/{id} - Should update project")
    void updateProject_Success() throws Exception {
        projectRequest.setName("Updated Dashboard");
        projectResponse.setName("Updated Dashboard");

        when(projectService.updateProject(org.mockito.ArgumentMatchers.eq("PRJ-101"), any(ProjectRequest.class)))
                .thenReturn(projectResponse);

        mockMvc.perform(put("/api/v1/projects/PRJ-101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Dashboard"));
    }

    @Test
    @DisplayName("GET /projects/{id} - Should fetch project")
    void getProject_Success() throws Exception {
        when(projectService.getProject("PRJ-101")).thenReturn(projectResponse);

        mockMvc.perform(get("/api/v1/projects/PRJ-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("PRJ-101"));
    }

    @Test
    @DisplayName("GET /projects - Should fetch all active projects")
    void getAllActiveProjects_Success() throws Exception {
        when(projectService.getAllActiveProjects()).thenReturn(List.of(projectResponse));

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("PRJ-101"));
    }

    @Test
    @DisplayName("PATCH /projects/{id}/deactivate - Should deactivate project")
    void deactivateProject_Success() throws Exception {
        doNothing().when(projectService).deactivateProject("PRJ-101");

        mockMvc.perform(patch("/api/v1/projects/PRJ-101/deactivate"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /projects - Should return 400 for invalid data")
    void createProject_InvalidData() throws Exception {
        ProjectRequest invalidRequest = new ProjectRequest();

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
