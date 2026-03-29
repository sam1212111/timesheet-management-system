package com.tms.as.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tms.as.dto.AdminUpdateUserRequest;
import com.tms.as.dto.AdminUserDetailResponse;
import com.tms.as.dto.AdminUserListItemResponse;
import com.tms.as.dto.AuthResponse;
import com.tms.as.dto.LoginRequest;
import com.tms.as.dto.ManagerOptionResponse;
import com.tms.as.dto.RegisterRequest;
import com.tms.as.dto.UpdateProfileRequest;
import com.tms.as.dto.UserResponse;
import com.tms.as.entity.Role;
import com.tms.as.entity.Status;
import com.tms.as.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserResponse userResponse;
    private AuthResponse authResponse;
    private AdminUserListItemResponse adminUserListItemResponse;
    private AdminUserDetailResponse adminUserDetailResponse;
    private ManagerOptionResponse managerOptionResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        userResponse = new UserResponse();
        userResponse.setId("USR-ABC12345");
        userResponse.setFullName("John Doe");
        userResponse.setEmail("john@example.com");
        userResponse.setEmployeeCode("EMP-001");
        userResponse.setRole(Role.EMPLOYEE);
        userResponse.setStatus(Status.ACTIVE);
        userResponse.setCreatedAt(LocalDateTime.now());
        userResponse.setUpdatedAt(LocalDateTime.now());

        authResponse = new AuthResponse(
                "jwt-token", "USR-ABC12345", "john@example.com", "John Doe", "EMPLOYEE"
        );

        adminUserListItemResponse = new AdminUserListItemResponse();
        adminUserListItemResponse.setId("USR-ABC12345");
        adminUserListItemResponse.setFullName("John Doe");
        adminUserListItemResponse.setEmail("john@example.com");
        adminUserListItemResponse.setEmployeeCode("EMP-001");
        adminUserListItemResponse.setRole(Role.EMPLOYEE);
        adminUserListItemResponse.setStatus(Status.ACTIVE);
        adminUserListItemResponse.setManagerId("USR-MGR001");
        adminUserListItemResponse.setCreatedAt(LocalDateTime.now());
        adminUserListItemResponse.setUpdatedAt(LocalDateTime.now());

        adminUserDetailResponse = new AdminUserDetailResponse();
        adminUserDetailResponse.setId("USR-ABC12345");
        adminUserDetailResponse.setFullName("John Doe");
        adminUserDetailResponse.setEmail("john@example.com");
        adminUserDetailResponse.setEmployeeCode("EMP-001");
        adminUserDetailResponse.setRole(Role.EMPLOYEE);
        adminUserDetailResponse.setStatus(Status.ACTIVE);
        adminUserDetailResponse.setManagerId("USR-MGR001");
        adminUserDetailResponse.setCreatedAt(LocalDateTime.now());
        adminUserDetailResponse.setUpdatedAt(LocalDateTime.now());

        managerOptionResponse = new ManagerOptionResponse();
        managerOptionResponse.setId("USR-MGR001");
        managerOptionResponse.setFullName("Jane Manager");
        managerOptionResponse.setEmail("manager@example.com");
        managerOptionResponse.setRole(Role.MANAGER);
    }

    @Test
    @DisplayName("POST /register should register successfully")
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setEmployeeCode("EMP-001");
        request.setPassword("Password1!");

        when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("USR-ABC12345"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("POST /login should login successfully")
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("Password1!");

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.id").value("USR-ABC12345"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("GET /profile/{id} should return profile")
    void getProfile_Success() throws Exception {
        when(authService.getProfile(anyString(), anyString())).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/auth/profile/USR-ABC12345")
                        .principal(new UsernamePasswordAuthenticationToken("john@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("USR-ABC12345"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    @DisplayName("PUT /profile/{id} should update profile")
    void updateProfile_Success() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("John Updated");
        request.setEmail("john@example.com");

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId("USR-ABC12345");
        updatedResponse.setFullName("John Updated");
        updatedResponse.setEmail("john@example.com");
        updatedResponse.setEmployeeCode("EMP-001");
        updatedResponse.setRole(Role.EMPLOYEE);
        updatedResponse.setStatus(Status.ACTIVE);

        when(authService.updateProfile(anyString(), any(UpdateProfileRequest.class), anyString()))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/auth/profile/USR-ABC12345")
                        .principal(new UsernamePasswordAuthenticationToken("john@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Updated"));
    }

    @Test
    @DisplayName("PUT /admin/users/{id} should admin update user")
    void adminUpdateUser_Success() throws Exception {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFullName("John Admin Updated");
        request.setEmail("john@example.com");
        request.setEmployeeCode("EMP-001");
        request.setRole(Role.MANAGER);
        request.setStatus(Status.ACTIVE);

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId("USR-ABC12345");
        updatedResponse.setFullName("John Admin Updated");
        updatedResponse.setEmail("john@example.com");
        updatedResponse.setRole(Role.MANAGER);
        updatedResponse.setStatus(Status.ACTIVE);

        when(authService.adminUpdateUser(anyString(), any(AdminUpdateUserRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/auth/admin/users/USR-ABC12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Admin Updated"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("GET /admin/users should return admin user list")
    void getAdminUsers_Success() throws Exception {
        when(authService.getAdminUsers(Role.EMPLOYEE, Status.ACTIVE, "john"))
                .thenReturn(List.of(adminUserListItemResponse));

        mockMvc.perform(get("/api/v1/auth/admin/users")
                        .param("role", "EMPLOYEE")
                        .param("status", "ACTIVE")
                        .param("search", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("USR-ABC12345"))
                .andExpect(jsonPath("$[0].managerId").value("USR-MGR001"));
    }

    @Test
    @DisplayName("GET /admin/users/{id} should return admin user detail")
    void getAdminUserById_Success() throws Exception {
        when(authService.getAdminUserById("USR-ABC12345")).thenReturn(adminUserDetailResponse);

        mockMvc.perform(get("/api/v1/auth/admin/users/USR-ABC12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("USR-ABC12345"))
                .andExpect(jsonPath("$.managerId").value("USR-MGR001"));
    }

    @Test
    @DisplayName("GET /admin/managers should return assignable managers")
    void getAssignableManagers_Success() throws Exception {
        when(authService.getAssignableManagers("jane")).thenReturn(List.of(managerOptionResponse));

        mockMvc.perform(get("/api/v1/auth/admin/managers")
                        .param("search", "jane"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("USR-MGR001"))
                .andExpect(jsonPath("$[0].role").value("MANAGER"));
    }

    @Test
    @DisplayName("POST /register should return 400 for invalid request")
    void register_InvalidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
