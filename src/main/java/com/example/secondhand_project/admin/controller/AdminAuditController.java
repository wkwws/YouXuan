package com.example.secondhand_project.admin.controller;

import com.example.secondhand_project.admin.context.AdminContext;
import com.example.secondhand_project.admin.dto.AdminPageQuery;
import com.example.secondhand_project.admin.dto.AuditActionDTO;
import com.example.secondhand_project.admin.entity.AdminAuditTask;
import com.example.secondhand_project.admin.service.AdminAuditTaskService;
import com.example.secondhand_project.admin.service.AdminPostService;
import com.example.secondhand_project.admin.service.AdminProductService;
import com.example.secondhand_project.admin.vo.AdminPageVO;
import com.example.secondhand_project.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/audit-tasks")
@Tag(name = "95-管理端审核队列", description = "待审核 UGC 队列")
public class AdminAuditController {

    private final AdminAuditTaskService auditTaskService;

    private final AdminProductService productService;

    private final AdminPostService postService;

    public AdminAuditController(AdminAuditTaskService auditTaskService,
                                AdminProductService productService,
                                AdminPostService postService) {
        this.auditTaskService = auditTaskService;
        this.productService = productService;
        this.postService = postService;
    }

    @Operation(summary = "审核任务分页")
    @GetMapping
    public Result<AdminPageVO<Map<String, Object>>> page(AdminPageQuery query) {
        return Result.success(auditTaskService.page(query));
    }

    @Operation(summary = "审核任务详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(auditTaskService.detail(id));
    }

    @Operation(summary = "通过/驳回审核任务")
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody AuditActionDTO dto) {
        if (dto == null || dto.getAction() == null || dto.getAction().isBlank()) {
            return Result.error("请指定审核结果");
        }
        AdminAuditTask task = auditTaskService.getById(id);
        if (task == null) {
            return Result.error("审核任务不存在");
        }
        if (task.getStatus() != null && task.getStatus() != 0) {
            return Result.error("该内容已被处理");
        }
        if ("PRODUCT".equals(task.getTargetType())) {
            productService.audit(task.getTargetId(), dto.getAction(), dto.getReason());
        } else if ("POST".equals(task.getTargetType())) {
            postService.audit(task.getTargetId(), dto.getAction(), dto.getReason());
        } else {
            return Result.error("不支持的内容类型");
        }
        return Result.success();
    }

    @Operation(summary = "待审核数量")
    @GetMapping("/pending-count")
    public Result<Long> pendingCount() {
        return Result.success(auditTaskService.countByStatus(0));
    }
}
