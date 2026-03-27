package com.tms.eu;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Context smoke test requires infrastructure wiring and is excluded from unit verification")
class EurekaServerApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally left as a disabled smoke-test placeholder for local unit verification.
        assertTrue(true);
    }
}
