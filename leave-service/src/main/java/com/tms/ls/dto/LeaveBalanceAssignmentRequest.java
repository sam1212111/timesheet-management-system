package com.tms.ls.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class LeaveBalanceAssignmentRequest {

    @NotBlank
    private String employeeId;

    @Valid
    @NotEmpty
    private List<LeaveBalanceAssignmentItem> assignments;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public List<LeaveBalanceAssignmentItem> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<LeaveBalanceAssignmentItem> assignments) {
        this.assignments = assignments;
    }
}
