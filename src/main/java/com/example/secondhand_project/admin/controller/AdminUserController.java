package com.example.secondhand_project.admin.controller;

import com.example.secondhand_project.admin.dto.AdminPageQuery;
import com.example.secondhand_project.admin.dto.AdminUserUpdateDTO;
import com.example.secondhand_project.admin.dto.BanDTO;
import com.example.secondhand_project.admin.service.AdminUserService;
import com.example.secondhand_project.admin.vo.AdminPageVO;
import com.example.secondhand_project.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/users")
@Tag(name = "93-管理端用户", description = "用户查询、编辑、封禁、解封")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户分页")
    @GetMapping
    public Result<AdminPageVO<Map<String, Object>>> page(AdminPageQuery query) {
        return Result.success(userService.page(query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        return Result.success(userService.detail(id));
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody AdminUserUpdateDTO dto) {
        userService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "封禁用户")
    @PostMapping("/{id}/ban")
    public Result<Void> ban(@PathVariable Integer id, @RequestBody BanDTO dto) {
        userService.ban(id, dto == null ? null : dto.getReason(), dto == null ? null : dto.getDays());
        return Result.success();
    }

    @Operation(summary = "解封用户")
    @PostMapping("/{id}/unban")
    public Result<Void> unban(@PathVariable Integer id) {
        userService.unban(id);
        return Result.success();
    }

    @Operation(summary = "删除用户（无内容时）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        userService.delete(id);
        return Result.success();
    }
}
