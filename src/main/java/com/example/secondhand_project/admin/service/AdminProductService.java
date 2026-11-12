package com.example.secondhand_project.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.secondhand_project.admin.context.AdminContext;
import com.example.secondhand_project.admin.dto.AdminPageQuery;
import com.example.secondhand_project.admin.dto.AdminProductUpdateDTO;
import com.example.secondhand_project.admin.vo.AdminPageVO;
import com.example.secondhand_project.pojo.entity.Category;
import com.example.secondhand_project.pojo.entity.Product;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.service.ICategoryService;
import com.example.secondhand_project.service.IProductService;
import com.example.secondhand_project.service.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端商品 CRUD + 基础审核/下架/封禁。
 */
@Service
public class AdminProductService {

    private final IProductService productService;

    private final IUserService userService;

    private final ICategoryService categoryService;

    private final AdminAuditTaskService auditTaskService;

    private final AdminLogService logService;

    private final ObjectMapper objectMapper;

    public AdminProductService(IProductService productService, IUserService userService,
                               ICategoryService categoryService, AdminAuditTaskService auditTaskService,
                               AdminLogService logService, ObjectMapper objectMapper) {
        this.productService = productService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.auditTaskService = auditTaskService;
        this.logService = logService;
        this.objectMapper = objectMapper;
    }

    public AdminPageVO<Map<String, Object>> page(AdminPageQuery query) {
        int page = query.safePage();
        int pageSize = query.safePageSize();
        String keyword = query.trimmedKeyword();
        String status = query.getStatus() == null ? "" : query.getStatus().trim();

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        switch (status) {
            case "on" -> wrapper.eq(Product::getStatus, 0).ne(Product::getAuditStatus, 2);
            case "sold" -> wrapper.eq(Product::getStatus, 1).ne(Product::getAuditStatus, 2);
            case "off" -> wrapper.eq(Product::getStatus, 2).ne(Product::getAuditStatus, 2);
            case "banned" -> wrapper.eq(Product::getAuditStatus, 2);
            default -> { }
        }

        if (query.hasText(query.getCategory())) {
            Category category = categoryService.lambdaQuery()
                    .eq(Category::getName, query.getCategory().trim())
                    .one();
            wrapper.eq(Product::getCategory, category == null ? -1 : category.getId());
        }

        if (!keyword.isEmpty()) {
            List<Integer> sellerIds = userService.lambdaQuery()
                    .and(w -> w.like(User::getNickname, keyword).or()
                            .like(User::getUsername, keyword))
                    .list()
                    .stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            wrapper.and(w -> {
                w.like(Product::getTitle, keyword);
                if (!sellerIds.isEmpty()) {
                    w.or().in(Product::getUid, sellerIds);
                }
            });
        }

        wrapper.orderByDesc(Product::getCreatedAt);
        Page<Product> result = productService.page(new Page<>(page, pageSize), wrapper);
        List<Map<String, Object>> list = toVOList(result.getRecords());
        return AdminPageVO.of(list, result.getTotal(), page, pageSize);
    }

    public Map<String, Object> detail(Integer id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        User seller = product.getUid() == null ? null : userService.getById(product.getUid());
        Category category = product.getCategory() == null ? null : categoryService.getById(product.getCategory());
        return toVO(product, seller, category);
    }

    @Transactional
    public void update(Integer id, AdminProductUpdateDTO dto) {
        Product existing = productService.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (dto.getTitle() != null) {
            if (dto.getTitle().isBlank()) {
                throw new IllegalArgumentException("商品标题不能为空");
            }
            existing.setTitle(dto.getTitle().trim());
        }
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription().trim());
        }
        if (dto.getPrice() != null) {
            if (dto.getPrice().signum() < 0) {
                throw new IllegalArgumentException("价格不能为负数");
            }
            existing.setPrice(dto.getPrice());
        }
        if (dto.getCategory() != null) {
            existing.setCategory(dto.getCategory());
        }
        existing.setUpdatedAt(LocalDateTime.now())
                .setAuditStatus(0)
                .setAuditRemark("")
                .setAuditedBy(null)
                .setAuditedAt(null);
        productService.updateById(existing);

        auditTaskService.submit("PRODUCT", id, existing.getUid(), existing.getTitle(),
                existing.getDescription(), snapshot(existing));
        logService.record("商品管理", "编辑商品", "商品 #" + id + " " + existing.getTitle());
    }

    @Transactional
    public void delete(Integer id) {
        Product existing = productService.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        productService.removeById(id);
        auditTaskService.remove("PRODUCT", id);
        logService.record("商品管理", "删除商品", "商品 #" + id + " " + existing.getTitle());
    }

    public void offline(Integer id, String reason) {
        Product product = requireProduct(id);
        product.setStatus(2).setOfflineReason(requireReason(reason, "下架原因")).setUpdatedAt(LocalDateTime.now());
        productService.updateById(product);
        logService.record("商品管理", "下架商品", "商品 #" + id + " " + product.getTitle());
    }

    public void restore(Integer id) {
        Product product = requireProduct(id);
        product.setStatus(0).setOfflineReason("").setUpdatedAt(LocalDateTime.now());
        productService.updateById(product);
        logService.record("商品管理", "恢复商品", "商品 #" + id + " " + product.getTitle());
    }

    public void ban(Integer id, String reason) {
        Product product = requireProduct(id);
        String text = requireReason(reason, "封禁原因");
        product.setStatus(2)
                .setAuditStatus(2)
                .setAuditRemark(text)
                .setOfflineReason(text)
                .setAuditedBy(AdminContext.getId())
                .setAuditedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());
        productService.updateById(product);
        auditTaskService.mark("PRODUCT", id, 2, text, AdminContext.getId());
        logService.record("商品管理", "封禁商品", "商品 #" + id + " " + product.getTitle());
    }

    public void unban(Integer id) {
        Product product = requireProduct(id);
        product.setStatus(0)
                .setAuditStatus(1)
                .setAuditRemark("")
                .setOfflineReason("")
                .setAuditedBy(AdminContext.getId())
                .setAuditedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());
        productService.updateById(product);
        auditTaskService.mark("PRODUCT", id, 1, "", AdminContext.getId());
        logService.record("商品管理", "解封商品", "商品 #" + id + " " + product.getTitle());
    }

    @Transactional
    public void audit(Integer id, String action, String reason) {
        Product product = requireProduct(id);
        boolean approve = "approve".equalsIgnoreCase(action) || "pass".equalsIgnoreCase(action);
        String remark = approve ? "" : requireReason(reason, "驳回原因");
        product.setAuditedBy(AdminContext.getId())
                .setAuditedAt(LocalDateTime.now())
                .setAuditRemark(remark)
                .setUpdatedAt(LocalDateTime.now());
        if (approve) {
            product.setAuditStatus(1);
        } else {
            product.setAuditStatus(2).setStatus(2).setOfflineReason(remark);
        }
        productService.updateById(product);
        auditTaskService.mark("PRODUCT", id, approve ? 1 : 2, remark, AdminContext.getId());
        logService.record("内容审核", approve ? "通过商品" : "驳回商品",
                "商品 #" + id + " " + product.getTitle());
    }

    public List<Category> categories() {
        return categoryService.lambdaQuery()
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId)
                .list();
    }

    private Product requireProduct(Integer id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        return product;
    }

    private String requireReason(String reason, String label) {
        if (reason == null || reason.trim().length() < 2) {
            throw new IllegalArgumentException("请填写至少 2 个字的" + label);
        }
        return reason.trim();
    }

    private List<Map<String, Object>> toVOList(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Integer> uids = products.stream()
                .map(Product::getUid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Integer> categoryIds = products.stream()
                .map(Product::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, User> users = uids.isEmpty() ? Collections.emptyMap()
                : userService.listByIds(uids).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<Integer, Category> categories = categoryIds.isEmpty() ? Collections.emptyMap()
                : categoryService.listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, Function.identity(), (a, b) -> a));
        return products.stream()
                .map(product -> toVO(product, users.get(product.getUid()), categories.get(product.getCategory())))
                .collect(Collectors.toList());
    }

    private Map<String, Object> toVO(Product product, User seller, Category category) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.getId());
        map.put("title", product.getTitle());
        map.put("sellerId", product.getUid());
        map.put("seller", publicUser(seller));
        map.put("categoryId", product.getCategory());
        map.put("category", category == null ? "" : category.getName());
        map.put("price", product.getPrice());
        map.put("status", statusText(product));
        map.put("auditStatus", product.getAuditStatus());
        map.put("auditStatusText", auditStatusText(product.getAuditStatus()));
        map.put("auditRemark", product.getAuditRemark());
        int imageCount = product.getImages() == null ? 0 : product.getImages().size();
        map.put("images", imageCount);
        map.put("imageUrls", product.getImages() == null ? Collections.emptyList() : product.getImages());
        map.put("description", product.getDescription());
        map.put("createdAt", product.getCreatedAt());
        map.put("updatedAt", product.getUpdatedAt());
        map.put("offlineReason", product.getOfflineReason());
        return map;
    }

    private Map<String, Object> publicUser(User user) {
        if (user == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("avatar", user.getAvatar());
        map.put("avatarText", avatarText(user));
        return map;
    }

    private String avatarText(User user) {
        String source = user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername() : user.getNickname();
        return source == null || source.isBlank() ? "?" : source.substring(0, 1);
    }

    private String statusText(Product product) {
        if (product.getAuditStatus() != null && product.getAuditStatus() == 2) {
            return "banned";
        }
        if (product.getStatus() == null) {
            return "on";
        }
        return switch (product.getStatus()) {
            case 1 -> "sold";
            case 2 -> "off";
            default -> "on";
        };
    }

    private String auditStatusText(Integer status) {
        if (status == null) {
            return "待审核";
        }
        return switch (status) {
            case 1 -> "已通过";
            case 2 -> "已驳回";
            default -> "待审核";
        };
    }

    private String snapshot(Product product) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", product.getId());
            map.put("title", product.getTitle());
            map.put("description", product.getDescription());
            map.put("price", product.getPrice());
            map.put("images", product.getImages());
            map.put("uid", product.getUid());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }
}
