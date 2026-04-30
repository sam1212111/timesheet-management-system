package com.tms.as.controller;

import com.tms.as.dto.AdminUpdateUserRequest;
import com.tms.as.dto.AdminUserDetailResponse;
import com.tms.as.dto.AdminUserListItemResponse;
import com.tms.as.dto.AssignManagerRequest;
import com.tms.as.dto.AuthResponse;
import com.tms.as.dto.LoginRequest;
import com.tms.as.dto.ManagerOptionResponse;
import com.tms.as.dto.RegisterRequest;
import com.tms.as.dto.TeamMemberResponse;
import com.tms.as.dto.UpdateProfileRequest;
import com.tms.as.dto.UserResponse;
import com.tms.as.entity.Role;
import com.tms.as.entity.Status;
import com.tms.as.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new employee")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/profile/{id}")
    @Operation(summary = "Get user profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> getProfile(@PathVariable String id,
                                                    Authentication authentication) {
        return ResponseEntity.ok(authService.getProfile(id, authentication.getName()));
    }
    
    @PutMapping("/profile/{id}")
    @Operation(summary = "Update own profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> updateProfile(@PathVariable String id,
                                                       @Valid @RequestBody UpdateProfileRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(authService.updateProfile(id, request, authentication.getName()));
    }

    @PutMapping("/admin/users/{id}")
    @Operation(summary = "Admin update any user", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> adminUpdateUser(@PathVariable String id,
                                                         @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(authService.adminUpdateUser(id, request));
    }

    @PutMapping("/admin/users/{id}/manager")
    @Operation(summary = "Assign manager to a user", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> assignManager(@PathVariable String id,
                                                      @Valid @RequestBody AssignManagerRequest request) {
        return ResponseEntity.ok(authService.assignManager(id, request.getManagerId()));
    }

    @GetMapping("/admin/users")
    @Operation(summary = "List users for admin management", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminUserListItemResponse>> getAdminUsers(@RequestParam(required = false) Role role,
                                                                         @RequestParam(required = false) Status status,
                                                                         @RequestParam(required = false) String search) {
        return ResponseEntity.ok(authService.getAdminUsers(role, status, search));
    }

    @GetMapping("/admin/users/{id}")
    @Operation(summary = "Get user details for admin management", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserDetailResponse> getAdminUserById(@PathVariable String id) {
        return ResponseEntity.ok(authService.getAdminUserById(id));
    }

    @GetMapping("/admin/managers")
    @Operation(summary = "List assignable managers", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ManagerOptionResponse>> getAssignableManagers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(authService.getAssignableManagers(search));
    }

    @GetMapping("/team/members")
    @Operation(summary = "List team members for manager or admin", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(Authentication authentication,
                                                                   @RequestParam(required = false) String managerId,
                                                                   @RequestParam(required = false) String search) {
        return ResponseEntity.ok(authService.getTeamMembers(authentication.getName(), managerId, search));
    }
    
    @GetMapping("/users/{employeeId}/manager")
    @Operation(summary = "Get manager ID for employee",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> getManagerForEmployee(@PathVariable String employeeId) {
        return ResponseEntity.ok(authService.getManagerForEmployee(employeeId));
    }
}
