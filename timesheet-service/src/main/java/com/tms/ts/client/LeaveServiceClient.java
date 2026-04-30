package com.tms.ts.client;

import com.tms.ts.client.dto.HolidayClientResponse;
import com.tms.ts.client.dto.LeaveRequestClientResponse;
import com.tms.ts.client.fallback.LeaveFallback;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "leave-service", fallback = LeaveFallback.class)
public interface LeaveServiceClient {

    @GetMapping("/api/v1/leave/requests")
    List<LeaveRequestClientResponse> getMyLeaveRequests(
            @RequestHeader("X-User-Id") String employeeId,
            @RequestHeader("Authorization") String authorization);

    @GetMapping("/api/v1/leave/holidays")
    List<HolidayClientResponse> getHolidays(
            @RequestHeader("Authorization") String authorization);
}
