package com.tms.ts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tms.ts.dto.TimesheetEntryRequest;
import com.tms.ts.dto.TimesheetEntryResponse;
import com.tms.ts.dto.TimesheetResponse;
import com.tms.ts.dto.TimesheetSubmitRequest;
import com.tms.ts.dto.TimesheetValidationResponse;
import com.tms.ts.entity.TimesheetStatus;
import com.tms.ts.exception.GlobalExceptionHandler;
import com.tms.ts.service.TimesheetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TimesheetControllerTest {

    @Mock
    private TimesheetService timesheetService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private TimesheetEntryResponse entryResponse;
    private TimesheetResponse timesheetResponse;
    private TimesheetValidationResponse validationResponse;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(new TimesheetController(timesheetService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        entryResponse = new TimesheetEntryResponse();
        entryResponse.setId("TE-001");
        entryResponse.setProjectId("PRJ-101");
        entryResponse.setProjectName("Test Project");
        entryResponse.setHoursWorked(new BigDecimal("8.0"));

        timesheetResponse = new TimesheetResponse();
        timesheetResponse.setId("TS-001");
        timesheetResponse.setStatus(TimesheetStatus.DRAFT);
        timesheetResponse.setEntries(List.of(entryResponse));

        validationResponse = new TimesheetValidationResponse(true, new ArrayList<>(), new BigDecimal("40.0"));
    }

    @Test
    @DisplayName("POST /entries - Should add a new entry")
    void addEntry_Success() throws Exception {
        TimesheetEntryRequest request = new TimesheetEntryRequest();
        request.setProjectId("PRJ-101");
        request.setWorkDate(today);
        request.setHoursWorked(new BigDecimal("8.0"));
        request.setTaskSummary("Did some work");

        when(timesheetService.addEntry(any(TimesheetEntryRequest.class), anyString(), anyString()))
                .thenReturn(entryResponse);

        mockMvc.perform(post("/api/v1/timesheets/entries")
                        .header("X-User-Id", "USR-123")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("TE-001"))
                .andExpect(jsonPath("$.projectName").value("Test Project"));
    }

    @Test
    @DisplayName("PUT /entries/{entryId} - Should update an entry")
    void updateEntry_Success() throws Exception {
        TimesheetEntryRequest request = new TimesheetEntryRequest();
        request.setProjectId("PRJ-101");
        request.setWorkDate(today);
        request.setHoursWorked(new BigDecimal("4.0"));

        entryResponse.setHoursWorked(new BigDecimal("4.0"));

        when(timesheetService.updateEntry(anyString(), any(TimesheetEntryRequest.class), anyString()))
                .thenReturn(entryResponse);

        mockMvc.perform(put("/api/v1/timesheets/entries/TE-001")
                        .header("X-User-Id", "USR-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoursWorked").value(4.0));
    }

    @Test
    @DisplayName("DELETE /entries/{entryId} - Should delete an entry")
    void deleteEntry_Success() throws Exception {
        doNothing().when(timesheetService).deleteEntry("TE-001", "USR-123");

        mockMvc.perform(delete("/api/v1/timesheets/entries/TE-001")
                        .header("X-User-Id", "USR-123"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/timesheets - Should fetch timesheet by date")
    void getTimesheet_Success() throws Exception {
        LocalDate requestedDate = today.with(java.time.DayOfWeek.WEDNESDAY);
        LocalDate weekStart = requestedDate.with(java.time.DayOfWeek.MONDAY);

        when(timesheetService.getTimesheet(weekStart, "USR-123"))
                .thenReturn(timesheetResponse);

        mockMvc.perform(get("/api/v1/timesheets")
                        .param("date", requestedDate.toString())
                        .header("X-User-Id", "USR-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("TS-001"));
    }

    @Test
    @DisplayName("GET /weeks/{date}/validate - Should successfully validate timesheet")
    void validateTimesheet_Success() throws Exception {
        LocalDate requestedDate = today.with(java.time.DayOfWeek.THURSDAY);
        LocalDate weekStart = requestedDate.with(java.time.DayOfWeek.MONDAY);

        when(timesheetService.validateTimesheet(weekStart, "USR-123"))
                .thenReturn(validationResponse);

        mockMvc.perform(get("/api/v1/timesheets/weeks/" + requestedDate + "/validate")
                        .header("X-User-Id", "USR-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.totalWeeklyHours").value(40.0));
    }

    @Test
    @DisplayName("GET /all - Should fetch all timesheets for employee")
    void getAllTimesheets_Success() throws Exception {
        when(timesheetService.getAllTimesheets("USR-123"))
                .thenReturn(List.of(timesheetResponse));

        mockMvc.perform(get("/api/v1/timesheets/all")
                        .header("X-User-Id", "USR-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("TS-001"));
    }

    @Test
    @DisplayName("POST /submit - Should submit a timesheet")
    void submitTimesheet_Success() throws Exception {
        TimesheetSubmitRequest request = new TimesheetSubmitRequest();
        request.setWeekStart(today);
        request.setComments("Week done");

        timesheetResponse.setStatus(TimesheetStatus.SUBMITTED);

        when(timesheetService.submitTimesheet(any(TimesheetSubmitRequest.class), anyString(), anyString()))
                .thenReturn(timesheetResponse);

        mockMvc.perform(post("/api/v1/timesheets/submit")
                        .header("X-User-Id", "USR-123")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }
}
