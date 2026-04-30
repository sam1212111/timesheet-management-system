package com.tms.ls.service;

import com.tms.ls.dto.LeaveBalanceResponse;
import com.tms.ls.dto.LeaveBalanceAssignmentRequest;
import com.tms.ls.dto.LeaveRequestDto;
import com.tms.ls.dto.LeaveResponse;
import com.tms.ls.dto.TeamCalendarResponse;

import java.util.List;

public interface LeaveService {
    List<LeaveBalanceResponse> getBalances(String employeeId);
    List<LeaveBalanceResponse> getBalancesForEmployee(String employeeId, String requesterId, String requesterRole, String authorization);
    List<LeaveBalanceResponse> assignBalances(LeaveBalanceAssignmentRequest request);
    LeaveResponse requestLeave(LeaveRequestDto requestDto, String employeeId, String authorization);
    List<LeaveResponse> getMyRequests(String employeeId);
    LeaveResponse getLeaveRequestById(String id);
    TeamCalendarResponse getTeamCalendar(String managerId);
    void initializeBalances(String employeeId);
    void approveLeave(String id, java.util.Map<String, String> comments);
    void rejectLeave(String id, java.util.Map<String, String> comments);
    void cancelLeaveRequest(String id, String employeeId);
}
