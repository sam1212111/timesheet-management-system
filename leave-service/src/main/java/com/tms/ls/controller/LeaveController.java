package com.tms.ls.controller;

import com.tms.ls.dto.LeaveBalanceResponse;
import com.tms.ls.dto.LeaveBalanceAssignmentRequest;
import com.tms.ls.dto.LeaveRequestDto;
import com.tms.ls.dto.LeaveResponse;
import com.tms.ls.dto.TeamCalendarResponse;
import com.tms.ls.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/leave")
@SecurityRequirement(name = "bearerAuth")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/balances")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyBalances(@io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId) {
        return ResponseEntity.ok(leaveService.getBalances(employeeId));
    }

    @GetMapping("/balances/{employeeId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<LeaveBalanceResponse>> getEmployeeBalances(
            @PathVariable String employeeId,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("X-User-Id") String requesterId,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("X-User-Role") String requesterRole,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(leaveService.getBalancesForEmployee(employeeId, requesterId, requesterRole, authorization));
    }

    @PostMapping("/balances/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaveBalanceResponse>> assignBalances(@Valid @RequestBody LeaveBalanceAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.assignBalances(request));
    }

    @PostMapping("/requests")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<LeaveResponse> requestLeave(@Valid @RequestBody LeaveRequestDto request,
                                                      @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId,
                                                      @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.requestLeave(request, employeeId, authorization));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<LeaveResponse>> getMyRequests(@io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId) {
        return ResponseEntity.ok(leaveService.getMyRequests(employeeId));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<LeaveResponse> getLeaveRequestById(@PathVariable String id) {
        return ResponseEntity.ok(leaveService.getLeaveRequestById(id));
    }
    
    @PostMapping("/balances/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> initializeBalances(@RequestParam String employeeId) {
        leaveService.initializeBalances(employeeId);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/team-calendar")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<TeamCalendarResponse> getTeamCalendar(@io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId) {
        return ResponseEntity.ok(leaveService.getTeamCalendar(employeeId));
    }

    @PatchMapping("/requests/{id}/approve")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> approveLeave(@PathVariable String id, @RequestBody java.util.Map<String, String> comments) {
        leaveService.approveLeave(id, comments);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/requests/{id}/reject")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> rejectLeave(@PathVariable String id, @RequestBody java.util.Map<String, String> comments) {
        leaveService.rejectLeave(id, comments);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/requests/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> cancelLeaveRequest(@PathVariable String id, @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId) {
        leaveService.cancelLeaveRequest(id, employeeId);
        return ResponseEntity.ok().build();
    }
}
