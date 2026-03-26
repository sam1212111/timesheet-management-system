package com.tms.admin.controller;

import com.tms.admin.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Dashboard", description = "Dashboard and summary endpoints for admins, managers, and employees")
public class AdminDashboardController {

    private final ReportingService reportingService;

    public AdminDashboardController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/compliance")
    @Operation(summary = "Get compliance dashboard", description = "Returns high-level compliance metrics for managers and admins.")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getComplianceDashboard() {
        return ResponseEntity.ok(reportingService.getComplianceDashboard());
    }

    @GetMapping("/employee-summary")
    @Operation(summary = "Get employee summary dashboard", description = "Returns the summary dashboard visible to employees, managers, and admins.")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEmployeeSummaryDashboard(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") String employeeId) {
        return ResponseEntity.ok(reportingService.getEmployeeSummaryDashboard(employeeId));
    }
}
