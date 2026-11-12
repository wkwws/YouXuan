package com.example.secondhand_project.admin.service;

import com.example.secondhand_project.admin.context.AdminContext;
import com.example.secondhand_project.conf.JwtConf;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.service.IUserService;
import com.example.secondhand_project.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 管理端登录。复用 user 表与现有 MD5 密码，只允许 role >= 1 的账号。
 */
@Service
public class AdminAuthService {

    private final IUserService userService;

    private final JwtConf jwtConf;

    public AdminAuthService(IUserService userService, JwtConf jwtConf) {
        this.userService = userService;
        this.jwtConf = jwtConf;
    }

    public Map<String, Object> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("请输入账号和密码");
        }
        User user = userService.lambdaQuery()
                .eq(User::getUsername, username.trim())
                .one();
        if (user == null) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        String encoded = DigestUtils.md5DigestAsHex(password.getBytes());
        if (user.getPwd() == null || !user.getPwd().equals(encoded)) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        if (user.getRole() == null || user.getRole() < 1) {
            throw new IllegalArgumentException("该账号不是管理员");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new IllegalArgumentException("管理员账号已被停用");
        }

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("id", user.getId().longValue());
        claims.put("role", user.getRole());
        claims.put("type", "admin");
        claims.put("jti", UUID.randomUUID().toString());
        String token = JwtUtil.createJWT(jwtConf.getSecretKey(), jwtConf.getTtlMills(), claims);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("admin", adminView(user));
        return result;
    }

    public Map<String, Object> me() {
        Integer id = AdminContext.getId();
        User user = id == null ? null : userService.getById(id);
        if (user == null) {
            throw new IllegalArgumentException("管理员账号不存在");
        }
        return adminView(user);
    }

    private Map<String, Object> adminView(User user) {
        Map<String, Object> admin = new LinkedHashMap<>();
        admin.put("id", user.getId());
        admin.put("name", user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername() : user.getNickname());
        admin.put("username", user.getUsername());
        admin.put("nickname", user.getNickname());
        admin.put("avatar", user.getAvatar());
        admin.put("role", user.getRole());
        admin.put("permissions", List.of("*"));
        return admin;
    }
}
