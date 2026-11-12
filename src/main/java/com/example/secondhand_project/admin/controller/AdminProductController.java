package com.example.secondhand_project.admin.controller;

import com.example.secondhand_project.admin.dto.AdminPageQuery;
import com.example.secondhand_project.admin.dto.AdminProductUpdateDTO;
import com.example.secondhand_project.admin.dto.AuditActionDTO;
import com.example.secondhand_project.admin.dto.ReasonDTO;
import com.example.secondhand_project.admin.service.AdminProductService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/products")
@Tag(name = "91-管理端商品", description = "商品查询、编辑、删除、审核、下架")
public class AdminProductController {

    private final AdminProductService productService;

    public AdminProductController(AdminProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "商品分页")
    @GetMapping
    public Result<AdminPageVO<Map<String, Object>>> page(AdminPageQuery query) {
        return Result.success(productService.page(query));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        return Result.success(productService.detail(id));
    }

    @Operation(summary = "编辑商品")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody AdminProductUpdateDTO dto) {
        productService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        productService.delete(id);
        return Result.success();
    }

    @Operation(summary = "审核商品")
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Integer id, @RequestBody AuditActionDTO dto) {
        if (dto == null || dto.getAction() == null || dto.getAction().isBlank()) {
            return Result.error("请指定审核结果");
        }
        productService.audit(id, dto.getAction(), dto.getReason());
        return Result.success();
    }

    @Operation(summary = "下架商品")
    @PostMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Integer id, @RequestBody(required = false) ReasonDTO dto) {
        productService.offline(id, dto == null ? null : dto.getReason());
        return Result.success();
    }

    @Operation(summary = "恢复上架")
    @PostMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Integer id) {
        productService.restore(id);
        return Result.success();
    }

    @Operation(summary = "封禁商品")
    @PostMapping("/{id}/ban")
    public Result<Void> ban(@PathVariable Integer id, @RequestBody(required = false) ReasonDTO dto) {
        productService.ban(id, dto == null ? null : dto.getReason());
        return Result.success();
    }

    @Operation(summary = "解封商品")
    @PostMapping("/{id}/unban")
    public Result<Void> unban(@PathVariable Integer id) {
        productService.unban(id);
        return Result.success();
    }

    @Operation(summary = "分类选项")
    @GetMapping("/categories/options")
    public Result<List<Map<String, Object>>> categories() {
        List<Map<String, Object>> list = productService.categories().stream().map(category -> {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", category.getId());
            item.put("name", category.getName());
            item.put("val", category.getVal());
            item.put("icon", category.getIcon());
            return item;
        }).toList();
        return Result.success(list);
    }
}
