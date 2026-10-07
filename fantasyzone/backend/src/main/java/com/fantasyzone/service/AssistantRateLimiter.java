package com.fantasyzone.service;

import com.fantasyzone.exception.LlmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Limits how many assistant questions each user can ask per hour, because every question costs money. */
@Component
public class AssistantRateLimiter {
    private static final long WINDOW_MS = 3_600_000;

    private final int maxPerHour;
    private final LongSupplier clock;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Autowired
    public AssistantRateLimiter(@Value("${llm.rate-limit-per-hour:20}") int maxPerHour) {
        this(maxPerHour, System::currentTimeMillis);
    }

    AssistantRateLimiter(int maxPerHour, LongSupplier clock) {
        this.maxPerHour = Math.max(1, maxPerHour);
        this.clock = clock;
    }

    /** Records a question for this user, or throws a 429 if they are over the limit. */
    public void check(String user) {
        long now = clock.getAsLong();
        Deque<Long> window = hits.computeIfAbsent(user.toLowerCase(), k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > WINDOW_MS) window.pollFirst();
            if (window.size() >= maxPerHour) {
                throw new LlmException(HttpStatus.TOO_MANY_REQUESTS,
                    "You have reached the limit of " + maxPerHour + " assistant questions per hour. Please try again later.", null);
            }
            window.addLast(now);
        }
    }
}
