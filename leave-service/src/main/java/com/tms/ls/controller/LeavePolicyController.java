package com.tms.ls.controller;

import com.tms.ls.dto.LeavePolicyDto;
import com.tms.ls.entity.LeavePolicy;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.service.LeavePolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/leave/policies")
@SecurityRequirement(name = "bearerAuth")
public class LeavePolicyController {

    private final LeavePolicyService leavePolicyService;

    public LeavePolicyController(LeavePolicyService leavePolicyService) {
        this.leavePolicyService = leavePolicyService;
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<LeavePolicy>> getAllPolicies() {
        return ResponseEntity.ok(leavePolicyService.getAllPolicies());
    }

    @GetMapping("/{type}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<LeavePolicy> getPolicyByType(@PathVariable LeaveType type) {
        return ResponseEntity.ok(leavePolicyService.getPolicyByType(type));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeavePolicy> createOrUpdatePolicy(@Valid @RequestBody LeavePolicyDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leavePolicyService.createOrUpdatePolicy(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePolicy(@PathVariable String id) {
        leavePolicyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
