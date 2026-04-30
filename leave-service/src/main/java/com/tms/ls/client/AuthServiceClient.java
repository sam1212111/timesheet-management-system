package com.tms.ls.client;

import com.tms.ls.client.dto.TeamMemberClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import com.tms.ls.client.fallback.AuthFallback;


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
