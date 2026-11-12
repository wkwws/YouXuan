package com.example.secondhand_project.admin.controller;

import com.example.secondhand_project.admin.service.AdminDashboardService;
import com.example.secondhand_project.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@Tag(name = "96-管理工作台", description = "统计卡片、趋势、分类分布、待处理事项")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "工作台概览")
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.success(dashboardService.summary());
    }

    @Operation(summary = "新增趋势")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(
            @RequestParam(value = "days", required = false, defaultValue = "7") Integer days) {
        return Result.success(dashboardService.trend(days == null ? 7 : days));
    }

    @Operation(summary = "商品分类分布")
    @GetMapping("/category-distribution")
    public Result<List<Map<String, Object>>> categoryDistribution() {
        return Result.success(dashboardService.categoryDistribution());
    }

    @Operation(summary = "待处理事项")
    @GetMapping("/pending")
    public Result<List<Map<String, Object>>> pending(
            @RequestParam(value = "limit", required = false, defaultValue = "4") Integer limit) {
        return Result.success(dashboardService.pending(limit == null ? 4 : limit));
    }
}
