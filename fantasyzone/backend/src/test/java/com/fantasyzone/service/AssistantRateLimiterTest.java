package com.fantasyzone.service;

import com.fantasyzone.exception.LlmException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AssistantRateLimiterTest {
    private final AtomicLong now = new AtomicLong(1_000_000);
    private final AssistantRateLimiter limiter = new AssistantRateLimiter(2, now::get);

    @Test
    void blocksTheQuestionAfterTheLimit() {
        limiter.check("a@test.com");
        limiter.check("a@test.com");
        LlmException e = assertThrows(LlmException.class, () -> limiter.check("a@test.com"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
    }

    @Test
    void eachUserHasTheirOwnAllowance() {
        limiter.check("a@test.com");
        limiter.check("a@test.com");
        assertDoesNotThrow(() -> limiter.check("b@test.com"));
    }

    @Test
    void emailCaseDoesNotCreateANewAllowance() {
        limiter.check("a@test.com");
        limiter.check("A@Test.com");
        assertThrows(LlmException.class, () -> limiter.check("a@TEST.com"));
    }

    @Test
    void allowanceComesBackAfterAnHour() {
        limiter.check("a@test.com");
        limiter.check("a@test.com");
        now.addAndGet(3_600_001);
        assertDoesNotThrow(() -> limiter.check("a@test.com"));
    }
}
