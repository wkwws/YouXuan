package com.example.secondhand_project.admin.service;

import com.example.secondhand_project.admin.dto.AdminCategoryDTO;
import com.example.secondhand_project.pojo.entity.Category;
import com.example.secondhand_project.pojo.entity.Product;
import com.example.secondhand_project.service.ICategoryService;
import com.example.secondhand_project.service.IProductService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类基础 CRUD。分类是 C 端发布商品的必选项，后台需要能维护。
 */
@Service
public class AdminCategoryService {

    private final ICategoryService categoryService;

    private final IProductService productService;

    private final AdminLogService logService;

    public AdminCategoryService(ICategoryService categoryService, IProductService productService,
                                AdminLogService logService) {
        this.categoryService = categoryService;
        this.productService = productService;
        this.logService = logService;
    }

    public List<Map<String, Object>> list() {
        return categoryService.lambdaQuery()
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId)
                .list()
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> create(AdminCategoryDTO dto) {
        validate(dto, null);
        Category category = new Category()
                .setName(dto.getName().trim())
                .setVal(dto.getVal().trim())
                .setIcon(dto.getIcon() == null ? "" : dto.getIcon().trim())
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .setCreatedAt(LocalDateTime.now());
        categoryService.save(category);
        logService.record("分类管理", "新增分类", category.getName());
        return toVO(category);
    }

    public Map<String, Object> update(Integer id, AdminCategoryDTO dto) {
        Category category = requireCategory(id);
        validate(dto, id);
        category.setName(dto.getName().trim())
                .setVal(dto.getVal().trim())
                .setIcon(dto.getIcon() == null ? "" : dto.getIcon().trim())
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        categoryService.updateById(category);
        logService.record("分类管理", "编辑分类", category.getName());
        return toVO(category);
    }

    public void delete(Integer id) {
        Category category = requireCategory(id);
        long count = productService.lambdaQuery().eq(Product::getCategory, id).count();
        if (count > 0) {
            throw new IllegalArgumentException("该分类下还有 " + count + " 件商品，不能删除");
        }
        categoryService.removeById(id);
        logService.record("分类管理", "删除分类", category.getName());
    }

    private Category requireCategory(Integer id) {
        Category category = categoryService.getById(id);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        return category;
    }

    private void validate(AdminCategoryDTO dto, Integer excludeId) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        if (dto.getVal() == null || dto.getVal().isBlank()) {
            throw new IllegalArgumentException("分类标识不能为空");
        }
        long sameVal = categoryService.lambdaQuery()
                .eq(Category::getVal, dto.getVal().trim())
                .ne(excludeId != null, Category::getId, excludeId)
                .count();
        if (sameVal > 0) {
            throw new IllegalArgumentException("分类标识已存在");
        }
    }

    private Map<String, Object> toVO(Category category) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", category.getId());
        map.put("name", category.getName());
        map.put("val", category.getVal());
        map.put("icon", category.getIcon());
        map.put("sortOrder", category.getSortOrder());
        map.put("createdAt", category.getCreatedAt());
        return map;
    }
}
