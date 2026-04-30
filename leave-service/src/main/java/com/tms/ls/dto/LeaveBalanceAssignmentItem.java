package com.tms.ls.dto;

import com.tms.ls.entity.LeaveType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class LeaveBalanceAssignmentItem {

    @NotNull
    private LeaveType leaveType;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal totalAllowed;

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public BigDecimal getTotalAllowed() {
        return totalAllowed;
    }

    public void setTotalAllowed(BigDecimal totalAllowed) {
        this.totalAllowed = totalAllowed;
    }
}
