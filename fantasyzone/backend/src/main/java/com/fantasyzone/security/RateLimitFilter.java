package com.fantasyzone.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Simple in-memory limit on auth requests per minute per client address (default 10). */
public class RateLimitFilter extends OncePerRequestFilter {
    private final int maxRequests;
    private static final long WINDOW_MS = 60_000;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimitFilter(int maxRequests) { this.maxRequests = maxRequests; }

    /** Behind CloudFront, the last X-Forwarded-For entry is the viewer address as CloudFront saw it. */
    private static String clientAddress(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) return req.getRemoteAddr();
        String[] parts = forwarded.split(",");
        return parts[parts.length - 1].trim();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        Deque<Long> window = hits.computeIfAbsent(clientAddress(req), k -> new ArrayDeque<>());
        boolean limited;
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > WINDOW_MS) window.pollFirst();
            limited = window.size() >= maxRequests;
            if (!limited) window.addLast(now);
        }
        if (limited) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Try again in a minute\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
