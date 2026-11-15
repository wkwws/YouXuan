package com.example.secondhand_project.aspect;

import com.example.secondhand_project.annotation.RateLimit;
import com.example.secondhand_project.constants.RedisConstants;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.enums.RateLimitDimension;
import com.example.secondhand_project.exception.RateLimitException;
import com.example.secondhand_project.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面：解析请求者身份，执行 Lua 滑动窗口脚本，超限抛 {@link RateLimitException}。
 *
 * <p>Redis 异常时 fail-open（记录日志并放行），避免限流组件放大故障。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAspect {

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();

    static {
        RATE_LIMIT_SCRIPT.setLocation(new ClassPathResource("lua/rate_limit.lua"));
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String identity = resolveIdentity(rateLimit.dimension());
        if (identity == null) {
            // USER 维度但拿不到登录用户：无法识别主体，直接放行
            return joinPoint.proceed();
        }

        Signature signature = joinPoint.getSignature();
        String bizKey = rateLimit.key().isBlank()  //拼接 redis 的key名称 ，如： Controller.getusername
                ? signature.getDeclaringType().getSimpleName() + "." + signature.getName()//getDeclaringType 的作用就是获取
                : rateLimit.key();                                                        //该方法的所属类名
        String redisKey = RedisConstants.RATE_LIMIT_KEY + bizKey + ":" + identity;

        Long allowed;
        try {
            long now = System.currentTimeMillis();
            allowed = stringRedisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(redisKey),
                    String.valueOf(now),
                    String.valueOf(TimeUnit.SECONDS.toMillis(rateLimit.window())),
                    String.valueOf(rateLimit.count()),
                    now + ":" + UUID.randomUUID()
            );
        } catch (Exception e) {
            log.warn("限流检查失败，放行请求：key={}", redisKey, e);
            return joinPoint.proceed();
        }

        // 只有脚本明确返回 0 才拒绝；null 等异常返回值按 fail-open 处理
        if (Long.valueOf(0L).equals(allowed)) {
            throw new RateLimitException(rateLimit.message());
        }
        return joinPoint.proceed();
    }

    /**
     * 解析限流身份；USER 维度拿不到用户时返回 null，表示放行。
     */
    private String resolveIdentity(RateLimitDimension dimension) {
        Long userId = BaseContext.getCurrentId();
        if (dimension == RateLimitDimension.USER) {
            return userId == null ? null : "u:" + userId;
        }
        if (dimension == RateLimitDimension.USER_OR_IP && userId != null) {
            return "u:" + userId;
        }
        return "ip:" + IpUtil.getClientIp(currentRequest());
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
