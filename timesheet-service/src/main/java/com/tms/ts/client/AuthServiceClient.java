package com.tms.ts.client;

import com.tms.ts.client.dto.TeamMemberClientResponse;
import java.util.List;
import com.tms.ts.client.fallback.AuthFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", fallback = AuthFallback.class)
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/users/{employeeId}/manager")
    String getManagerIdForEmployee(@PathVariable("employeeId") String employeeId,
                                   @RequestHeader("Authorization") String authorization);

    @GetMapping("/api/v1/auth/team/members")
    List<TeamMemberClientResponse> getTeamMembers(@RequestHeader("Authorization") String authorization,
                                                  @RequestParam(value = "managerId", required = false) String managerId,
                                                  @RequestParam(value = "search", required = false) String search);
}
