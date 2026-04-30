package com.tms.as.service;

import com.tms.as.dto.*;
import com.tms.as.entity.Role;
import com.tms.as.entity.Status;
import java.util.List;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getProfile(String id, String loggedInEmail);

    UserResponse updateProfile(String id, UpdateProfileRequest request, String loggedInEmail);

    UserResponse adminUpdateUser(String id, AdminUpdateUserRequest request);

    UserResponse assignManager(String id, String managerId);
    
    String getManagerForEmployee(String employeeId);

    List<AdminUserListItemResponse> getAdminUsers(Role role, Status status, String search);

    AdminUserDetailResponse getAdminUserById(String id);

    List<ManagerOptionResponse> getAssignableManagers(String search);

    List<TeamMemberResponse> getTeamMembers(String loggedInEmail, String managerId, String search);
}
