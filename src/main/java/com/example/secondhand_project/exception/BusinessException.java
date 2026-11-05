package com.example.secondhand_project.exception;

/**
 * 通用业务异常：内容不存在、无权操作、参数不合法等。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

}
