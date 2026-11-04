package com.example.secondhand_project.handler;

import com.example.secondhand_project.exception.LoginException;
import com.example.secondhand_project.exception.NullUser;
import com.example.secondhand_project.exception.RegisterException;
import com.example.secondhand_project.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 业务异常统一转为 Result{code:0,msg}，让前端能展示“密码错误 / 用户名已存在”等提示。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({LoginException.class, NullUser.class, RegisterException.class})
    public Result<Void> handleBusiness(RuntimeException e) {
        return Result.error(e.getMessage());
    }
}
