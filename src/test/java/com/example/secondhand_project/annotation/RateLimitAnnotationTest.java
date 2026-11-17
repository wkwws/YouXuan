package com.example.secondhand_project.annotation;

import com.example.secondhand_project.enums.RateLimitDimension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RateLimitAnnotationTest {

    @RateLimit
    private void defaultLimit() {
    }

    @RateLimit(count = 5, window = 30, key = "auth:login",
            dimension = RateLimitDimension.IP, message = "慢一点")
    private void customLimit() {
    }

    @Test
    void shouldProvideDefaultValues() throws NoSuchMethodException {
        RateLimit rateLimit = RateLimitAnnotationTest.class
                .getDeclaredMethod("defaultLimit")
                .getAnnotation(RateLimit.class);

        assertNotNull(rateLimit);
        assertEquals(20, rateLimit.count());
        assertEquals(60, rateLimit.window());
        assertEquals(RateLimitDimension.USER_OR_IP, rateLimit.dimension());
        assertEquals("", rateLimit.key());
        assertEquals("请求过于频繁，请稍后再试", rateLimit.message());
    }

    @Test
    void shouldKeepCustomValues() throws NoSuchMethodException {
        RateLimit rateLimit = RateLimitAnnotationTest.class
                .getDeclaredMethod("customLimit")
                .getAnnotation(RateLimit.class);

        assertNotNull(rateLimit);
        assertEquals(5, rateLimit.count());
        assertEquals(30, rateLimit.window());
        assertEquals(RateLimitDimension.IP, rateLimit.dimension());
        assertEquals("auth:login", rateLimit.key());
        assertEquals("慢一点", rateLimit.message());
    }
}
