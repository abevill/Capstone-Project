package com.fantasyzone.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-unit-test-secret-1234567890";

    @Test
    void tokenRoundTrips() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        String token = jwt.generateToken("a@test.com");
        assertTrue(jwt.isValid(token));
        assertEquals("a@test.com", jwt.extractEmail(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        String token = jwt.generateToken("a@test.com");
        assertFalse(jwt.isValid(token.substring(0, token.length() - 3) + "abc"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String foreign = new JwtService("another-secret-another-secret-another-123456", 60_000).generateToken("a@test.com");
        assertFalse(new JwtService(SECRET, 60_000).isValid(foreign));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, -1_000);
        assertFalse(jwt.isValid(jwt.generateToken("a@test.com")));
    }

    @Test
    void garbageIsRejected() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        assertFalse(jwt.isValid("not-a-jwt"));
        assertFalse(jwt.isValid(""));
    }
}
