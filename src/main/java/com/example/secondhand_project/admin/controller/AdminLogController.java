package com.example.secondhand_project.admin.controller;

import com.example.secondhand_project.admin.dto.AdminPageQuery;
import com.example.secondhand_project.admin.service.AdminLogService;
import com.example.secondhand_project.admin.vo.AdminPageVO;
import com.example.secondhand_project.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/operation-logs")
@Tag(name = "97-操作日志", description = "后台操作记录查询")
public class AdminLogController {

    private final AdminLogService logService;

    public AdminLogController(AdminLogService logService) {
        this.logService = logService;
    }

    @Operation(summary = "操作日志分页")
    @GetMapping
    public Result<AdminPageVO<Map<String, Object>>> page(AdminPageQuery query) {
        return Result.success(logService.page(query));
    }
}
