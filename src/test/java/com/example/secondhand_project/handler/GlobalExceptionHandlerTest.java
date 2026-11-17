package com.example.secondhand_project.handler;

import com.example.secondhand_project.exception.RateLimitException;
import com.example.secondhand_project.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    @Test
    void shouldConvertRateLimitExceptionToResult() {
        Result<Void> result = new GlobalExceptionHandler()
                .handleRateLimit(new RateLimitException("请求过于频繁，请稍后再试"));

        assertEquals(0, result.getCode().intValue());
        assertEquals("请求过于频繁，请稍后再试", result.getMsg());
    }

    @Test
    void shouldDeclareTooManyRequestsStatus() throws NoSuchMethodException {
        Method method = GlobalExceptionHandler.class
                .getMethod("handleRateLimit", RateLimitException.class);
        ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);

        assertNotNull(responseStatus);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, responseStatus.value());
    }
}
