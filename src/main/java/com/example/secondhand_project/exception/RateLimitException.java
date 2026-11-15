package com.example.secondhand_project.exception;

/**
 * 触发限流时抛出，由 GlobalExceptionHandler 统一转换为 HTTP 429。
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
