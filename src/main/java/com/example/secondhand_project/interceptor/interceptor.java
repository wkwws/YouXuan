package com.example.secondhand_project.interceptor;

import com.example.secondhand_project.conf.JwtConf;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.mapper.UserMapper;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class interceptor implements HandlerInterceptor {

    @Autowired
    private JwtConf jwtConf;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 公开 GET：允许匿名访问；但如果带了有效 token，也要解析出用户，用于返回 liked 状态
        boolean publicGet = "GET".equalsIgnoreCase(method)
                && (uri.startsWith("/posts")
                || uri.startsWith("/products")
                || uri.startsWith("/comments"));

        String token = request.getHeader(jwtConf.getUserTokenName());
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            if (publicGet) {
                return true;
            }
            response.setStatus(401);
            return false;
        }

        Claims claims;
        try {
            claims = JwtUtil.parseJWT(jwtConf.getSecretKey(), token);
        } catch (Exception e) {
            if (publicGet) {
                return true;
            }
            response.setStatus(401);
            return false;
        }

        Object id = claims == null ? null : claims.get("id");
        if (!(id instanceof Number)) {
            if (publicGet) {
                return true;
            }
            response.setStatus(401);
            return false;
        }

        User user = userMapper.selectById(((Number) id).intValue());
        if (user == null) {
            if (publicGet) {
                return true;
            }
            response.setStatus(401);
            return false;
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            response.setStatus(403);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"账号已被封禁\",\"data\":null}");
            return false;
        }

        // 将用户信息存到 ThreadLocal 中
        BaseContext.setCurrentId(((Number) id).longValue());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 结束后清理 ThreadLocal
        BaseContext.removeCurrentId();
    }

}
