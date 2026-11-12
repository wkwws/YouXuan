package com.example.secondhand_project.admin.interceptor;

import com.example.secondhand_project.admin.context.AdminContext;
import com.example.secondhand_project.conf.JwtConf;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.service.IUserService;
import com.example.secondhand_project.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端鉴权：只拦截 /admin/**，与 C 端拦截器完全分开。
 */
@Component
public class AdminJwtInterceptor implements HandlerInterceptor {

    private final JwtConf jwtConf;

    private final IUserService userService;

    public AdminJwtInterceptor(JwtConf jwtConf, IUserService userService) {
        this.jwtConf = jwtConf;
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(jwtConf.getUserTokenName());
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isBlank()) {
            return reject(response, 401, "请先登录管理后台");
        }

        Claims claims;
        try {
            claims = JwtUtil.parseJWT(jwtConf.getSecretKey(), token);
        } catch (Exception e) {
            return reject(response, 401, "登录已过期，请重新登录");
        }

        Object idObj = claims == null ? null : claims.get("id");
        if (!(idObj instanceof Number)) {
            return reject(response, 401, "登录凭证无效");
        }

        User user = userService.getById(((Number) idObj).intValue());
        if (user == null) {
            return reject(response, 401, "管理员账号不存在");
        }
        if (user.getRole() == null || user.getRole() < 1) {
            return reject(response, 403, "该账号没有管理权限");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            return reject(response, 403, "管理员账号已被停用");
        }

        AdminContext.set(new AdminContext.AdminInfo(
                user.getId(), user.getRole(), user.getUsername(), user.getNickname()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        AdminContext.clear();
    }

    private boolean reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":0,\"msg\":\"" + message + "\",\"data\":null}");
        return false;
    }
}
