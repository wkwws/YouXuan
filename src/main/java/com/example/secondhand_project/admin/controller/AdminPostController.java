package com.example.secondhand_project.admin.controller;

import com.example.secondhand_project.admin.dto.AdminPageQuery;
import com.example.secondhand_project.admin.dto.AuditActionDTO;
import com.example.secondhand_project.admin.dto.ReasonDTO;
import com.example.secondhand_project.admin.service.AdminPostService;
import com.example.secondhand_project.admin.vo.AdminPageVO;
import com.example.secondhand_project.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/posts")
@Tag(name = "92-管理端说说", description = "说说查询、删除、隐藏、审核")
public class AdminPostController {

    private final AdminPostService postService;

    public AdminPostController(AdminPostService postService) {
        this.postService = postService;
    }

    @Operation(summary = "说说分页")
    @GetMapping
    public Result<AdminPageVO<Map<String, Object>>> page(AdminPageQuery query) {
        return Result.success(postService.page(query));
    }

    @Operation(summary = "说说详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        return Result.success(postService.detail(id));
    }

    @Operation(summary = "删除说说")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        postService.delete(id);
        return Result.success();
    }

    @Operation(summary = "隐藏说说")
    @PostMapping("/{id}/hide")
    public Result<Void> hide(@PathVariable Integer id, @RequestBody(required = false) ReasonDTO dto) {
        postService.hide(id, dto == null ? null : dto.getReason());
        return Result.success();
    }

    @Operation(summary = "恢复说说")
    @PostMapping("/{id}/unhide")
    public Result<Void> unhide(@PathVariable Integer id) {
        postService.unhide(id);
        return Result.success();
    }

    @Operation(summary = "审核说说")
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Integer id, @RequestBody AuditActionDTO dto) {
        if (dto == null || dto.getAction() == null || dto.getAction().isBlank()) {
            return Result.error("请指定审核结果");
        }
        postService.audit(id, dto.getAction(), dto.getReason());
        return Result.success();
    }
}
