package com.example.secondhand_project.annotation;

import com.example.secondhand_project.enums.RateLimitDimension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解，标注在需要限流的方法上。
 *
 * <p>示例：{@code @RateLimit(count = 5, window = 60)} 表示 60 秒内同一请求者最多 5 次。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 窗口内允许的最大请求数 */
    int count() default 20;

    /** 窗口大小（秒） */
    int window() default 60;

    /** 限流维度 */
    RateLimitDimension dimension() default RateLimitDimension.USER_OR_IP;

    /** 自定义业务标识；为空时使用「类简名.方法名」 */
    String key() default "";

    /** 超限提示语 */
    String message() default "请求过于频繁，请稍后再试";
}
