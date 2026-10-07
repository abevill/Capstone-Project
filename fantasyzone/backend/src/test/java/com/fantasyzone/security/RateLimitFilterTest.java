package com.fantasyzone.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitFilterTest {
    private int call(RateLimitFilter filter, String uri) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("POST", uri), res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void blocksAuthRequestsOverTheLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2);
        assertEquals(200, call(filter, "/api/auth/login"));
        assertEquals(200, call(filter, "/api/auth/login"));
        assertEquals(429, call(filter, "/api/auth/login"));
    }

    @Test
    void doesNotLimitOtherEndpoints() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1);
        for (int i = 0; i < 5; i++) assertEquals(200, call(filter, "/api/players"));
    }

    @Test
    void eachForwardedClientGetsItsOwnLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1);
        for (String ip : new String[] {"1.1.1.1", "2.2.2.2"}) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.addHeader("X-Forwarded-For", "spoofed, " + ip);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }
    }
}
