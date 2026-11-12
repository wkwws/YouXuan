package com.example.secondhand_project.admin.controller;

import com.example.secondhand_project.admin.service.AdminAuthService;
import com.example.secondhand_project.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
@Tag(name = "90-管理端认证", description = "管理员登录、当前管理员信息")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return Result.success(adminAuthService.login(body.get("username"), body.get("password")));
    }

    @Operation(summary = "当前管理员信息")
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.success(adminAuthService.me());
    }

    @Operation(summary = "退出登录（前端清 token 即可）")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
