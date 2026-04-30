package com.tms.ts.client.fallback;

import com.tms.ts.client.AuthServiceClient;
import com.tms.ts.client.dto.TeamMemberClientResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthFallback implements AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthFallback.class);

    @Override
    public String getManagerIdForEmployee(String employeeId, String authorization) {
        log.warn("Auth Service is unreachable for getManagerIdForEmployee fallback. EmployeeId: {}", employeeId);
        return "";
    }

    @Override
    public List<TeamMemberClientResponse> getTeamMembers(String authorization, String managerId, String search) {
        log.warn("Auth Service is unreachable for getTeamMembers fallback. ManagerId: {}, search: {}", managerId, search);
        return List.of();
    }
}
