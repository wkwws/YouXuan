package com.example.secondhand_project.aspect;

import com.example.secondhand_project.annotation.RateLimit;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.enums.RateLimitDimension;
import com.example.secondhand_project.exception.RateLimitException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

    private StringRedisTemplate stringRedisTemplate;
    private ProceedingJoinPoint joinPoint;
    private Signature signature;
    private RateLimit rateLimit;
    private RateLimitAspect aspect;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        joinPoint = mock(ProceedingJoinPoint.class);
        signature = mock(Signature.class);
        rateLimit = mock(RateLimit.class);
        aspect = new RateLimitAspect(stringRedisTemplate);

        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(RateLimitAspectTest.class);
        when(signature.getName()).thenReturn("demo");
        when(rateLimit.count()).thenReturn(5);
        when(rateLimit.window()).thenReturn(60);
        when(rateLimit.dimension()).thenReturn(RateLimitDimension.USER_OR_IP);
        when(rateLimit.key()).thenReturn("");
        when(rateLimit.message()).thenReturn("请求过于频繁，请稍后再试");
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldProceedWhenRedisAllows() throws Throwable {
        BaseContext.setCurrentId(7L);
        stubRedisResult(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, rateLimit);

        assertEquals("ok", result);
        verify(joinPoint).proceed();
        assertEquals("rate_limit:RateLimitAspectTest.demo:u:7", capturedKey());
    }

    @Test
    void shouldThrowWhenRedisRejects() throws Throwable {
        BaseContext.setCurrentId(7L);
        stubRedisResult(0L);

        assertThrows(RateLimitException.class, () -> aspect.around(joinPoint, rateLimit));

        verify(joinPoint, never()).proceed();
    }

    @Test
    void shouldProceedWhenRedisReturnsNull() throws Throwable {
        BaseContext.setCurrentId(7L);
        stubRedisResult(null);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, rateLimit);

        assertEquals("ok", result);
        verify(joinPoint).proceed();
    }

    @Test
    void shouldProceedWhenRedisFails() throws Throwable {
        BaseContext.setCurrentId(7L);
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenThrow(new RedisConnectionFailureException("redis down"));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, rateLimit);

        assertEquals("ok", result);
        verify(joinPoint).proceed();
    }

    @Test
    void shouldFallbackToIpWhenNoUser() throws Throwable {
        BaseContext.removeCurrentId();
        request.setRemoteAddr("10.1.2.3");
        stubRedisResult(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.around(joinPoint, rateLimit);

        assertEquals("rate_limit:RateLimitAspectTest.demo:ip:10.1.2.3", capturedKey());
    }

    @Test
    void shouldProceedWithoutRedisWhenUserDimensionHasNoUser() throws Throwable {
        BaseContext.removeCurrentId();
        when(rateLimit.dimension()).thenReturn(RateLimitDimension.USER);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, rateLimit);

        assertEquals("ok", result);
        verify(joinPoint).proceed();
        verifyNoInteractions(stringRedisTemplate);
    }

    private void stubRedisResult(Long result) {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(result);
    }

    @SuppressWarnings("unchecked")
    private String capturedKey() {
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(stringRedisTemplate)
                .execute(any(RedisScript.class), captor.capture(), any(), any(), any(), any());
        return captor.getValue().get(0);
    }
}
