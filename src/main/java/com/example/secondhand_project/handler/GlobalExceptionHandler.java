package com.example.secondhand_project.handler;

import com.example.secondhand_project.exception.BusinessException;
import com.example.secondhand_project.exception.LoginException;
import com.example.secondhand_project.exception.NullUser;
import com.example.secondhand_project.exception.RateLimitException;
import com.example.secondhand_project.exception.RegisterException;
import com.example.secondhand_project.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 业务异常统一转为 Result{code:0,msg}，让前端能展示“密码错误 / 用户名已存在”等提示。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            LoginException.class,
            NullUser.class,
            RegisterException.class,
            BusinessException.class
    })
    public Result<Void> handleBusiness(RuntimeException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(e.getMessage() == null ? "请求参数有误" : e.getMessage());
    }

    /**
     * 限流异常统一返回 HTTP 429，响应体保持 Result 格式。
     */
    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<Void> handleRateLimit(RateLimitException e) {
        return Result.error(e.getMessage());
    }
}
