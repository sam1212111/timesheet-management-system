package com.tms.ts.repository;

import com.tms.ts.entity.Timesheet;
import com.tms.ts.entity.TimesheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, String> {

    Optional<Timesheet> findByEmployeeIdAndWeekStart(String employeeId, LocalDate weekStart);

    List<Timesheet> findByEmployeeId(String employeeId);

    List<Timesheet> findByEmployeeIdAndStatus(String employeeId, TimesheetStatus status);

    List<Timesheet> findByStatus(TimesheetStatus status);

    boolean existsByEmployeeIdAndWeekStart(String employeeId, LocalDate weekStart);

    @Query("""
            select t from Timesheet t
            where t.employeeId in :employeeIds
              and (:status is null or t.status = :status)
              and (:weekStart is null or t.weekStart = :weekStart)
            order by t.weekStart desc, t.updatedAt desc
            """)
    List<Timesheet> findTeamTimesheets(@Param("employeeIds") List<String> employeeIds,
                                       @Param("status") TimesheetStatus status,
                                       @Param("weekStart") LocalDate weekStart);
}
