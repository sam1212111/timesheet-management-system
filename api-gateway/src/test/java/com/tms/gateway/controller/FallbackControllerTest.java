package com.tms.gateway.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(FallbackController.class)
class FallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Auth fallback should return service unavailable payload")
    void authFallback_Success() {
        webTestClient.get()
                .uri("/fallback/auth")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SERVICE_UNAVAILABLE")
                .jsonPath("$.message").isEqualTo("Auth Service is temporarily unavailable")
                .jsonPath("$.timestamp").exists();
    }

    @Test
    @DisplayName("Timesheet fallback should return service unavailable payload")
    void timesheetFallback_Success() {
        webTestClient.get()
                .uri("/fallback/timesheet")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Timesheet Service is temporarily unavailable");
    }

    @Test
    @DisplayName("Leave fallback should return service unavailable payload")
    void leaveFallback_Success() {
        webTestClient.get()
                .uri("/fallback/leave")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Leave Service is temporarily unavailable");
    }

    @Test
    @DisplayName("Admin fallback should return service unavailable payload")
    void adminFallback_Success() {
        webTestClient.get()
                .uri("/fallback/admin")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Admin Service is temporarily unavailable");
    }
}
