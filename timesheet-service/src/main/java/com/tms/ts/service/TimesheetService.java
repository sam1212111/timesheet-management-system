package com.tms.ts.service;

import com.tms.ts.dto.*;
import com.tms.ts.entity.TimesheetStatus;
import java.time.LocalDate;
import java.util.List;

public interface TimesheetService {

    TimesheetEntryResponse addEntry(TimesheetEntryRequest request, String employeeId, String authorization);

    TimesheetEntryResponse updateEntry(String entryId, TimesheetEntryRequest request, String employeeId);

    void deleteEntry(String entryId, String employeeId);

    TimesheetResponse getTimesheet(LocalDate weekStart, String employeeId);
    TimesheetResponse getTimesheetById(String id);

    List<TimesheetResponse> getAllTimesheets(String employeeId);

    List<TeamTimesheetResponse> getTeamTimesheets(String authorization, String managerId, String search, TimesheetStatus status, LocalDate weekStart);

    TimesheetResponse submitTimesheet(TimesheetSubmitRequest request, String employeeId, String authorization);

    TimesheetValidationResponse validateTimesheet(LocalDate weekStart, String employeeId, String authorization);

    void approveTimesheet(String id, java.util.Map<String, String> comments);

    void rejectTimesheet(String id, java.util.Map<String, String> comments);
}
