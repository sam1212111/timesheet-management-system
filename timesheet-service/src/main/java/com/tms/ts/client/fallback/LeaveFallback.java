package com.tms.ts.client.fallback;

import com.tms.ts.client.LeaveServiceClient;
import com.tms.ts.client.dto.HolidayClientResponse;
import com.tms.ts.client.dto.LeaveRequestClientResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LeaveFallback implements LeaveServiceClient {

    private static final Logger log = LoggerFactory.getLogger(LeaveFallback.class);

    @Override
    public List<LeaveRequestClientResponse> getMyLeaveRequests(String employeeId, String authorization) {
        log.warn("Leave Service is unreachable for getMyLeaveRequests fallback. EmployeeId: {}", employeeId);
        return List.of();
    }

    @Override
    public List<HolidayClientResponse> getHolidays(String authorization) {
        log.warn("Leave Service is unreachable for getHolidays fallback.");
        return List.of();
    }
}
