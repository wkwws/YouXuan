package com.example.secondhand_project.admin.controller;

import com.example.secondhand_project.admin.dto.AdminCategoryDTO;
import com.example.secondhand_project.admin.service.AdminCategoryService;
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
@RequestMapping("/admin/categories")
@Tag(name = "94-管理端分类", description = "商品分类增删改查")
public class AdminCategoryController {

    private final AdminCategoryService categoryService;

    public AdminCategoryController(AdminCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类列表")
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.success(categoryService.list());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody AdminCategoryDTO dto) {
        return Result.success(categoryService.create(dto));
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Integer id, @RequestBody AdminCategoryDTO dto) {
        return Result.success(categoryService.update(id, dto));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return Result.success();
    }
}
