package com.tms.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        return buildFallbackResponse("Auth Service is temporarily unavailable");
    }

    @RequestMapping("/timesheet")
    public ResponseEntity<Map<String, Object>> timesheetFallback() {
        return buildFallbackResponse("Timesheet Service is temporarily unavailable");
    }

    @RequestMapping("/leave")
    public ResponseEntity<Map<String, Object>> leaveFallback() {
        return buildFallbackResponse("Leave Service is temporarily unavailable");
    }

    @RequestMapping("/admin")
    public ResponseEntity<Map<String, Object>> adminFallback() {
        return buildFallbackResponse("Admin Service is temporarily unavailable");
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", "SERVICE_UNAVAILABLE");
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}