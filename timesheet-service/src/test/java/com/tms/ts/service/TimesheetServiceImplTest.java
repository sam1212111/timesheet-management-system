package com.tms.ts.service;

import com.tms.ts.client.AuthServiceClient;
import com.tms.ts.client.LeaveServiceClient;
import com.tms.ts.client.dto.HolidayClientResponse;
import com.tms.ts.client.dto.LeaveRequestClientResponse;
import com.tms.common.exception.UnauthorizedException;
import com.tms.common.util.IdGeneratorUtil;
import com.tms.ts.dto.TimesheetEntryRequest;
import com.tms.ts.dto.TimesheetEntryResponse;
import com.tms.ts.dto.TimesheetResponse;
import com.tms.ts.dto.TimesheetSubmitRequest;
import com.tms.ts.dto.TimesheetValidationResponse;
import com.tms.ts.entity.Project;
import com.tms.ts.entity.Timesheet;
import com.tms.ts.entity.TimesheetEntry;
import com.tms.ts.entity.TimesheetStatus;
import com.tms.ts.event.TimesheetSubmittedEvent;
import com.tms.ts.repository.ProjectRepository;
import com.tms.ts.repository.TimesheetEntryRepository;
import com.tms.ts.repository.TimesheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetServiceImplTest {

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private TimesheetEntryRepository timesheetEntryRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private LeaveServiceClient leaveServiceClient;

    @Mock
    private IdGeneratorUtil idGeneratorUtil;

    @Mock
    private TimesheetSubmissionEventPublisher timesheetSubmissionEventPublisher;

    @InjectMocks
    private TimesheetServiceImpl timesheetService;

    private Timesheet testTimesheet;
    private Project testProject;
    private TimesheetEntry testEntry;
    private TimesheetEntryRequest addRequest;
    private final String employeeId = "USR-123";
    private final String authorization = "Bearer test-token";
    private final LocalDate today = LocalDate.now();
    private final LocalDate weekStart = today.with(DayOfWeek.MONDAY);

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId("PRJ-101");
        testProject.setName("Test Project");
        testProject.setActive(true);

        testTimesheet = new Timesheet();
        testTimesheet.setId("TS-001");
        testTimesheet.setEmployeeId(employeeId);
        testTimesheet.setWeekStart(weekStart);
        testTimesheet.setStatus(TimesheetStatus.DRAFT);

        testEntry = new TimesheetEntry();
        testEntry.setId("TE-001");
        testEntry.setTimesheet(testTimesheet);
        testEntry.setProjectId("PRJ-101");
        testEntry.setWorkDate(today);
        testEntry.setHoursWorked(new BigDecimal("8.0"));

        addRequest = new TimesheetEntryRequest();
        addRequest.setProjectId("PRJ-101");
        addRequest.setWorkDate(today);
        addRequest.setHoursWorked(new BigDecimal("8.0"));
        addRequest.setTaskSummary("Did some work");

        when(leaveServiceClient.getHolidays(anyString())).thenReturn(List.of());
        when(leaveServiceClient.getMyLeaveRequests(anyString(), anyString())).thenReturn(List.of());
    }

    // ==================== ADD ENTRY TESTS ====================

    @Nested
    @DisplayName("Add Entry Tests")
    class AddEntryTests {

        @Test
        @DisplayName("Should successfully add a new entry to a draft timesheet")
        void addEntry_Success() {
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
            when(timesheetRepository.findByEmployeeIdAndWeekStart(employeeId, weekStart))
                    .thenReturn(Optional.of(testTimesheet));
            when(timesheetEntryRepository.existsByTimesheetIdAndWorkDateAndProjectId("TS-001", today, "PRJ-101"))
                    .thenReturn(false);
            when(timesheetEntryRepository.getTotalHoursForEmployeeAndDate(employeeId, today))
                    .thenReturn(BigDecimal.ZERO);
            when(idGeneratorUtil.generateId("TE")).thenReturn("TE-001");
            when(timesheetEntryRepository.save(any(TimesheetEntry.class))).thenReturn(testEntry);

            TimesheetEntryResponse response = timesheetService.addEntry(addRequest, employeeId, authorization);

            assertNotNull(response);
            assertEquals("TE-001", response.getId());
            assertEquals("PRJ-101", response.getProjectId());
            assertEquals("Test Project", response.getProjectName());
            assertEquals(new BigDecimal("8.0"), response.getHoursWorked());
            verify(timesheetEntryRepository).save(any(TimesheetEntry.class));
        }

        @Test
        @DisplayName("Should fail when max hours exceeded")
        void addEntry_MaxHoursExceeded() {
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
            when(timesheetRepository.findByEmployeeIdAndWeekStart(employeeId, weekStart))
                    .thenReturn(Optional.of(testTimesheet));
            when(timesheetEntryRepository.existsByTimesheetIdAndWorkDateAndProjectId("TS-001", today, "PRJ-101"))
                    .thenReturn(false);
            // Already worked 20 hours today, trying to add 8 more.
            when(timesheetEntryRepository.getTotalHoursForEmployeeAndDate(employeeId, today))
                    .thenReturn(new BigDecimal("20.0"));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.addEntry(addRequest, employeeId, authorization));

            assertTrue(ex.getMessage().contains("Daily hours cannot exceed 24"));
            verify(timesheetEntryRepository, never()).save(any(TimesheetEntry.class));
        }

        @Test
        @DisplayName("Should throw exception if project is not active")
        void addEntry_InactiveProject() {
            testProject.setActive(false);
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.addEntry(addRequest, employeeId, authorization));

            assertEquals("Project is not active", ex.getMessage());
            verify(timesheetEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception if work date is in the future")
        void addEntry_FutureDate() {
            addRequest.setWorkDate(today.plusDays(1));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.addEntry(addRequest, employeeId, authorization));

            assertEquals("Work date cannot be in the future", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception if timesheet is already submitted")
        void addEntry_TimesheetSubmitted() {
            testTimesheet.setStatus(TimesheetStatus.SUBMITTED);
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
            when(timesheetRepository.findByEmployeeIdAndWeekStart(employeeId, weekStart))
                    .thenReturn(Optional.of(testTimesheet));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.addEntry(addRequest, employeeId, authorization));

            assertEquals("Cannot add entry to a submitted or approved timesheet", ex.getMessage());
        }

        @Test
        @DisplayName("Should create a draft timesheet when none exists")
        void addEntry_CreatesTimesheetWhenMissing() {
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
            when(timesheetRepository.findByEmployeeIdAndWeekStart(employeeId, weekStart))
                    .thenReturn(Optional.empty());
            when(authServiceClient.getManagerIdForEmployee(employeeId, authorization)).thenReturn("USR-MGR001");
            when(idGeneratorUtil.generateId("TS")).thenReturn("TS-NEW");
            when(idGeneratorUtil.generateId("TE")).thenReturn("TE-001");
            when(timesheetEntryRepository.existsByTimesheetIdAndWorkDateAndProjectId("TS-NEW", today, "PRJ-101"))
                    .thenReturn(false);
            when(timesheetEntryRepository.getTotalHoursForEmployeeAndDate(employeeId, today))
                    .thenReturn(BigDecimal.ZERO);

            Timesheet createdTimesheet = new Timesheet();
            createdTimesheet.setId("TS-NEW");
            createdTimesheet.setEmployeeId(employeeId);
            createdTimesheet.setWeekStart(weekStart);
            createdTimesheet.setStatus(TimesheetStatus.DRAFT);
            createdTimesheet.setManagerId("USR-MGR001");

            TimesheetEntry createdEntry = new TimesheetEntry();
            createdEntry.setId("TE-001");
            createdEntry.setTimesheet(createdTimesheet);
            createdEntry.setProjectId("PRJ-101");
            createdEntry.setWorkDate(today);
            createdEntry.setHoursWorked(new BigDecimal("8.0"));

            when(timesheetRepository.save(any(Timesheet.class))).thenReturn(createdTimesheet);
            when(timesheetEntryRepository.save(any(TimesheetEntry.class))).thenReturn(createdEntry);

            TimesheetEntryResponse response = timesheetService.addEntry(addRequest, employeeId, authorization);

            assertEquals("TE-001", response.getId());
            verify(timesheetRepository).save(any(Timesheet.class));
        }
    }

    // ==================== UPDATE ENTRY TESTS ====================

    @Nested
    @DisplayName("Update Entry Tests")
    class UpdateEntryTests {

        @Test
        @DisplayName("Should successfully update an existing entry")
        void updateEntry_Success() {
            when(timesheetEntryRepository.findById("TE-001")).thenReturn(Optional.of(testEntry));
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
            when(timesheetEntryRepository.getTotalHoursForEmployeeAndDate(employeeId, today))
                    .thenReturn(new BigDecimal("8.0"));
            when(timesheetEntryRepository.save(any(TimesheetEntry.class))).thenReturn(testEntry);

            TimesheetEntryResponse response = timesheetService.updateEntry("TE-001", addRequest, employeeId);

            assertNotNull(response);
            assertEquals("Test Project", response.getProjectName());
            verify(timesheetEntryRepository).save(any(TimesheetEntry.class));
        }

        @Test
        @DisplayName("Should throw exception if trying to update another user's entry")
        void updateEntry_NotOwnEntry() {
            when(timesheetEntryRepository.findById("TE-001")).thenReturn(Optional.of(testEntry));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.updateEntry("TE-001", addRequest, "OTHER-EMP"));

            assertEquals("You can only update your own entries", ex.getMessage());
            verify(timesheetEntryRepository, never()).save(any());
        }
    }

    // ==================== DELETE ENTRY TESTS ====================

    @Nested
    @DisplayName("Delete Entry Tests")
    class DeleteEntryTests {

        @Test
        @DisplayName("Should successfully delete an entry")
        void deleteEntry_Success() {
            when(timesheetEntryRepository.findById("TE-001")).thenReturn(Optional.of(testEntry));
            
            timesheetService.deleteEntry("TE-001", employeeId);

            verify(timesheetEntryRepository).delete(testEntry);
        }

        @Test
        @DisplayName("Should reject deleting another employee's entry")
        void deleteEntry_NotOwnEntry() {
            when(timesheetEntryRepository.findById("TE-001")).thenReturn(Optional.of(testEntry));

            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    () -> timesheetService.deleteEntry("TE-001", "OTHER-EMP"));

            assertEquals("You can only delete your own entries", exception.getMessage());
            verify(timesheetEntryRepository, never()).delete(any());
        }
    }

    // ==================== GET & VALIDATE TIMESHEET TESTS ====================

    @Nested
    @DisplayName("Get Timesheet Tests")
    class GetTimesheetTests {

        @Test
        @DisplayName("Should fetch timesheet with mapped entries")
        void getTimesheet_Success() {
            testTimesheet.setEntries(List.of(testEntry));
            when(timesheetRepository.findByEmployeeIdAndWeekStart(employeeId, weekStart))
                    .thenReturn(Optional.of(testTimesheet));
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));

            TimesheetResponse response = timesheetService.getTimesheet(weekStart, employeeId);

            assertNotNull(response);
            assertEquals("TS-001", response.getId());
            assertEquals(1, response.getEntries().size());
            assertEquals("Test Project", response.getEntries().get(0).getProjectName());
        }

        @Test
        @DisplayName("Validate timesheet with missing days")
        void validateTimesheet_MissingDays() {
            // Only adding entry for today
            testTimesheet.setEntries(List.of(testEntry));
            when(timesheetRepository.findByEmployeeIdAndWeekStart(employeeId, weekStart))
                    .thenReturn(Optional.of(testTimesheet));

            TimesheetValidationResponse response = timesheetService.validateTimesheet(weekStart, employeeId, authorization);

            assertFalse(response.isValid());
            long weekdaysValidated = java.time.temporal.ChronoUnit.DAYS.between(
                    weekStart,
                    (today.isBefore(weekStart.plusDays(4)) ? today : weekStart.plusDays(4)).plusDays(1)
            );
            assertEquals(Math.max(0, weekdaysValidated - 1), response.getErrors().size());
        }

        @Test
        @DisplayName("Should return all timesheets mapped for the employee")
        void getAllTimesheets_Success() {
            testTimesheet.setEntries(List.of(testEntry));
            when(timesheetRepository.findByEmployeeId(employeeId)).thenReturn(List.of(testTimesheet));
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));

            List<TimesheetResponse> response = timesheetService.getAllTimesheets(employeeId);

            assertEquals(1, response.size());
            assertEquals("TS-001", response.get(0).getId());
            assertEquals("Test Project", response.get(0).getEntries().get(0).getProjectName());
        }
    }

    // ==================== SUBMIT TIMESHEET TESTS ====================

    @Nested
    @DisplayName("Submit Timesheet Tests")
    class SubmitTimesheetTests {

        @Test
        @DisplayName("Should submit valid timesheet and send RabbitMQ event")
        void submitTimesheet_Success() {
            // Set up 5 entries for Monday-Friday to pass validation
            TimesheetEntry e1 = new TimesheetEntry(); e1.setWorkDate(weekStart); e1.setHoursWorked(new BigDecimal("8"));
            TimesheetEntry e2 = new TimesheetEntry(); e2.setWorkDate(weekStart.plusDays(1)); e2.setHoursWorked(new BigDecimal("8"));
            TimesheetEntry e3 = new TimesheetEntry(); e3.setWorkDate(weekStart.plusDays(2)); e3.setHoursWorked(new BigDecimal("8"));
            TimesheetEntry e4 = new TimesheetEntry(); e4.setWorkDate(weekStart.plusDays(3)); e4.setHoursWorked(new BigDecimal("8"));
            TimesheetEntry e5 = new TimesheetEntry(); e5.setWorkDate(weekStart.plusDays(4)); e5.setHoursWorked(new BigDecimal("8"));
            
            // Re-mocking the entries with valid IDs and projects to satisfy mapToEntryResponse downstream
            e1.setProjectId("PRJ-101"); e1.setTimesheet(testTimesheet);
            e2.setProjectId("PRJ-101"); e2.setTimesheet(testTimesheet);
            e3.setProjectId("PRJ-101"); e3.setTimesheet(testTimesheet);
            e4.setProjectId("PRJ-101"); e4.setTimesheet(testTimesheet);
            e5.setProjectId("PRJ-101"); e5.setTimesheet(testTimesheet);

            testTimesheet.setEntries(List.of(e1, e2, e3, e4, e5));
            TimesheetSubmitRequest submitRequest = new TimesheetSubmitRequest();
            submitRequest.setWeekStart(weekStart);
            submitRequest.setComments("Week done");

            when(timesheetRepository.findByEmployeeIdAndWeekStart(employeeId, weekStart))
                    .thenReturn(Optional.of(testTimesheet));
            when(timesheetRepository.save(any(Timesheet.class))).thenReturn(testTimesheet);
            when(projectRepository.findById("PRJ-101")).thenReturn(Optional.of(testProject));
            when(authServiceClient.getManagerIdForEmployee(employeeId, authorization)).thenReturn("USR-MGR001");

            TimesheetResponse response = timesheetService.submitTimesheet(submitRequest, employeeId, authorization);

            assertEquals(TimesheetStatus.SUBMITTED, response.getStatus());
            verify(timesheetRepository).save(testTimesheet);
            verify(timesheetSubmissionEventPublisher).publishTimesheetSubmittedEvent(any(TimesheetSubmittedEvent.class));
        }

        @Test
        @DisplayName("Should throw exception if validation fails while submitting (TC-03)")
        void submitTimesheet_MissingDays() {
            // Only 1 entry leads to validation failure
            testTimesheet.setEntries(List.of(testEntry));
            TimesheetSubmitRequest submitRequest = new TimesheetSubmitRequest();
            submitRequest.setWeekStart(weekStart);

            when(timesheetRepository.findByEmployeeIdAndWeekStart(employeeId, weekStart))
                    .thenReturn(Optional.of(testTimesheet));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.submitTimesheet(submitRequest, employeeId, authorization));

            assertTrue(ex.getMessage().contains("Cannot submit timesheet"));
            verify(timesheetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception if timesheet has no entries")
        void submitTimesheet_Empty() {
            testTimesheet.setEntries(Collections.emptyList());
            TimesheetSubmitRequest submitRequest = new TimesheetSubmitRequest();
            submitRequest.setWeekStart(weekStart);

            when(timesheetRepository.findByEmployeeIdAndWeekStart(employeeId, weekStart))
                    .thenReturn(Optional.of(testTimesheet));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.submitTimesheet(submitRequest, employeeId, authorization));

            assertEquals("Cannot submit an empty timesheet", ex.getMessage());
            verify(timesheetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject submission when no manager is assigned")
        void submitTimesheet_NoManagerAssigned() {
            TimesheetEntry e1 = new TimesheetEntry(); e1.setWorkDate(weekStart); e1.setHoursWorked(new BigDecimal("8")); e1.setProjectId("PRJ-101"); e1.setTimesheet(testTimesheet);
            TimesheetEntry e2 = new TimesheetEntry(); e2.setWorkDate(weekStart.plusDays(1)); e2.setHoursWorked(new BigDecimal("8")); e2.setProjectId("PRJ-101"); e2.setTimesheet(testTimesheet);
            TimesheetEntry e3 = new TimesheetEntry(); e3.setWorkDate(weekStart.plusDays(2)); e3.setHoursWorked(new BigDecimal("8")); e3.setProjectId("PRJ-101"); e3.setTimesheet(testTimesheet);
            TimesheetEntry e4 = new TimesheetEntry(); e4.setWorkDate(weekStart.plusDays(3)); e4.setHoursWorked(new BigDecimal("8")); e4.setProjectId("PRJ-101"); e4.setTimesheet(testTimesheet);
            TimesheetEntry e5 = new TimesheetEntry(); e5.setWorkDate(weekStart.plusDays(4)); e5.setHoursWorked(new BigDecimal("8")); e5.setProjectId("PRJ-101"); e5.setTimesheet(testTimesheet);
            testTimesheet.setEntries(List.of(e1, e2, e3, e4, e5));

            TimesheetSubmitRequest submitRequest = new TimesheetSubmitRequest();
            submitRequest.setWeekStart(weekStart);

            when(timesheetRepository.findByEmployeeIdAndWeekStart(employeeId, weekStart))
                    .thenReturn(Optional.of(testTimesheet));
            when(authServiceClient.getManagerIdForEmployee(employeeId, authorization)).thenReturn(" ");

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> timesheetService.submitTimesheet(submitRequest, employeeId, authorization));

            assertEquals("Cannot submit timesheet: No manager assigned to approve", ex.getMessage());
            verify(timesheetRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Should approve a timesheet and copy approver comments")
    void approveTimesheet_Success() {
        when(timesheetRepository.findById("TS-001")).thenReturn(Optional.of(testTimesheet));

        timesheetService.approveTimesheet("TS-001", java.util.Map.of("comments", "Approved"));

        assertEquals(TimesheetStatus.APPROVED, testTimesheet.getStatus());
        assertEquals("Approved", testTimesheet.getComments());
        assertNotNull(testTimesheet.getApprovedAt());
        verify(timesheetRepository).save(testTimesheet);
    }

    @Test
    @DisplayName("Should approve a timesheet even when comments are omitted")
    void approveTimesheet_WithoutComments() {
        when(timesheetRepository.findById("TS-001")).thenReturn(Optional.of(testTimesheet));

        timesheetService.approveTimesheet("TS-001", null);

        assertEquals(TimesheetStatus.APPROVED, testTimesheet.getStatus());
        assertNull(testTimesheet.getComments());
        verify(timesheetRepository).save(testTimesheet);
    }

    @Test
    @DisplayName("Should reject a timesheet and set the rejection reason")
    void rejectTimesheet_Success() {
        when(timesheetRepository.findById("TS-001")).thenReturn(Optional.of(testTimesheet));

        timesheetService.rejectTimesheet("TS-001", java.util.Map.of("comments", "Please fix Friday hours"));

        assertEquals(TimesheetStatus.REJECTED, testTimesheet.getStatus());
        assertEquals("Please fix Friday hours", testTimesheet.getRejectionReason());
        verify(timesheetRepository).save(testTimesheet);
    }

    @Test
    @DisplayName("Should reject a timesheet even when comments are omitted")
    void rejectTimesheet_WithoutComments() {
        when(timesheetRepository.findById("TS-001")).thenReturn(Optional.of(testTimesheet));

        timesheetService.rejectTimesheet("TS-001", null);

        assertEquals(TimesheetStatus.REJECTED, testTimesheet.getStatus());
        assertNull(testTimesheet.getRejectionReason());
        verify(timesheetRepository).save(testTimesheet);
    }
}
