package com.example.secondhand_project.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.secondhand_project.admin.context.AdminContext;
import com.example.secondhand_project.admin.dto.AdminPageQuery;
import com.example.secondhand_project.admin.dto.AdminUserUpdateDTO;
import com.example.secondhand_project.admin.vo.AdminPageVO;
import com.example.secondhand_project.admin.entity.AdminOperationLog;
import com.example.secondhand_project.pojo.entity.Comment;
import com.example.secondhand_project.pojo.entity.Post;
import com.example.secondhand_project.pojo.entity.Product;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.service.ICommentService;
import com.example.secondhand_project.service.IPostService;
import com.example.secondhand_project.service.IProductService;
import com.example.secondhand_project.service.IUserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端用户 CRUD + 封禁/解封。
 */
@Service
public class AdminUserService {

    private final IUserService userService;

    private final IProductService productService;

    private final IPostService postService;

    private final ICommentService commentService;

    private final AdminLogService logService;

    public AdminUserService(IUserService userService, IProductService productService,
                            IPostService postService, ICommentService commentService,
                            AdminLogService logService) {
        this.userService = userService;
        this.productService = productService;
        this.postService = postService;
        this.commentService = commentService;
        this.logService = logService;
    }

    public AdminPageVO<Map<String, Object>> page(AdminPageQuery query) {
        int page = query.safePage();
        int pageSize = query.safePageSize();
        String keyword = query.trimmedKeyword();
        String status = query.getStatus() == null ? "" : query.getStatus().trim();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if ("normal".equals(status)) {
            wrapper.eq(User::getStatus, 0);
        } else if ("banned".equals(status)) {
            wrapper.eq(User::getStatus, 1);
        }
        if (!keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or()
                    .like(User::getNickname, keyword).or()
                    .like(User::getEmail, keyword).or()
                    .eq(User::getId, parseId(keyword)));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userService.page(new Page<>(page, pageSize), wrapper);
        List<Map<String, Object>> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return AdminPageVO.of(list, result.getTotal(), page, pageSize);
    }

    public Map<String, Object> detail(Integer id) {
        User user = requireUser(id);
        Map<String, Object> map = toVO(user);

        List<Post> recentPosts = postService.lambdaQuery()
                .eq(Post::getUid, id)
                .orderByDesc(Post::getCreatedAt)
                .last("LIMIT 3")
                .list();
        map.put("recentPosts", recentPosts.stream().map(this::postBrief).collect(Collectors.toList()));

        List<Product> recentProducts = productService.lambdaQuery()
                .eq(Product::getUid, id)
                .orderByDesc(Product::getCreatedAt)
                .last("LIMIT 3")
                .list();
        map.put("recentProducts", recentProducts.stream().map(this::productBrief).collect(Collectors.toList()));

        List<AdminOperationLog> recentLogs = logService.lambdaQuery()
                .like(AdminOperationLog::getTarget, user.getNickname() == null
                        ? user.getUsername() : user.getNickname())
                .orderByDesc(AdminOperationLog::getCreatedAt)
                .last("LIMIT 3")
                .list();
        map.put("recentLogs", recentLogs.stream().map(log -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.getId());
            item.put("time", log.getCreatedAt());
            item.put("admin", log.getAdminName());
            item.put("action", log.getAction());
            item.put("target", log.getTarget());
            return item;
        }).collect(Collectors.toList()));
        return map;
    }

    public void update(Integer id, AdminUserUpdateDTO dto) {
        User user = requireUser(id);
        if (dto.getRole() != null && !dto.getRole().equals(user.getRole())) {
            if (!AdminContext.isSuperAdmin()) {
                throw new IllegalArgumentException("只有超级管理员可以调整角色");
            }
            if (id.equals(AdminContext.getId()) && dto.getRole() < 1) {
                throw new IllegalArgumentException("不能取消自己的管理员权限");
            }
            user.setRole(dto.getRole());
        }
        if (dto.getNickname() != null) {
            if (dto.getNickname().isBlank()) {
                throw new IllegalArgumentException("昵称不能为空");
            }
            user.setNickname(dto.getNickname().trim());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail().trim());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone().trim());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio().trim());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar().trim());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userService.updateById(user);
        logService.record("用户管理", "编辑用户", "用户 #" + id + " " + user.getNickname());
    }

    public void ban(Integer id, String reason, Integer days) {
        User user = requireUser(id);
        if (id.equals(AdminContext.getId())) {
            throw new IllegalArgumentException("不能封禁自己");
        }
        if (user.getRole() != null && user.getRole() >= 2 && !AdminContext.isSuperAdmin()) {
            throw new IllegalArgumentException("不能封禁超级管理员");
        }
        if (reason == null || reason.trim().length() < 2) {
            throw new IllegalArgumentException("请填写至少 2 个字的封禁原因");
        }
        LocalDateTime until = null;
        if (days != null && days > 0) {
            until = LocalDateTime.now().plusDays(days);
        }
        user.setStatus(1)
                .setBanReason(reason.trim())
                .setBannedAt(LocalDateTime.now())
                .setBannedUntil(until)
                .setUpdatedAt(LocalDateTime.now());
        userService.updateById(user);
        logService.record("用户管理", "封禁用户", "用户 #" + id + " " + user.getNickname());
    }

    public void unban(Integer id) {
        User user = requireUser(id);
        user.setStatus(0)
                .setBanReason("")
                .setBannedAt(null)
                .setBannedUntil(null)
                .setUpdatedAt(LocalDateTime.now());
        userService.updateById(user);
        logService.record("用户管理", "解封用户", "用户 #" + id + " " + user.getNickname());
    }

    public void delete(Integer id) {
        User user = requireUser(id);
        if (id.equals(AdminContext.getId())) {
            throw new IllegalArgumentException("不能删除自己");
        }
        if (user.getRole() != null && user.getRole() >= 1 && !AdminContext.isSuperAdmin()) {
            throw new IllegalArgumentException("只有超级管理员可以删除管理员账号");
        }
        long products = productService.lambdaQuery().eq(Product::getUid, id).count();
        long posts = postService.lambdaQuery().eq(Post::getUid, id).count();
        if (products > 0 || posts > 0) {
            throw new IllegalArgumentException("该用户仍有商品或说说，请先处理内容后再删除");
        }
        userService.removeById(id);
        logService.record("用户管理", "删除用户", "用户 #" + id + " " + user.getNickname());
    }

    private User requireUser(Integer id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private Integer parseId(String keyword) {
        try {
            return Integer.valueOf(keyword);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private Map<String, Object> toVO(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("avatar", user.getAvatar());
        map.put("avatarText", avatarText(user));
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("bio", user.getBio());
        map.put("role", user.getRole() == null ? 0 : user.getRole());
        map.put("roleText", roleText(user.getRole()));
        boolean banned = user.getStatus() != null && user.getStatus() == 1;
        map.put("status", banned ? "banned" : "normal");
        map.put("banReason", user.getBanReason());
        map.put("banUntil", banned
                ? (user.getBannedUntil() == null ? "永久" : user.getBannedUntil())
                : "");
        map.put("registerAt", user.getCreatedAt());
        map.put("lastActiveAt", user.getLastOnline());
        map.put("postCount", postService.lambdaQuery().eq(Post::getUid, user.getId()).count());
        map.put("productCount", productService.lambdaQuery().eq(Product::getUid, user.getId()).count());
        map.put("commentCount", commentService.lambdaQuery()
                .eq(Comment::getUid, user.getId())
                .eq(Comment::getStatus, 1)
                .count());
        return map;
    }

    private Map<String, Object> postBrief(Post post) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("content", post.getContent());
        map.put("createdAt", post.getCreatedAt());
        map.put("likes", post.getLikeCount() == null ? 0 : post.getLikeCount());
        map.put("comments", post.getCommentCount() == null ? 0 : post.getCommentCount());
        map.put("visibility", post.getVisible() != null && post.getVisible() == 0 ? "hidden" : "visible");
        return map;
    }

    private Map<String, Object> productBrief(Product product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.getId());
        map.put("title", product.getTitle());
        map.put("price", product.getPrice());
        map.put("createdAt", product.getCreatedAt());
        map.put("status", productStatus(product));
        return map;
    }

    private String productStatus(Product product) {
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

    private String avatarText(User user) {
        String source = user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername() : user.getNickname();
        return source == null || source.isBlank() ? "?" : source.substring(0, 1);
    }

    private String roleText(Integer role) {
        if (role == null) {
            return "普通用户";
        }
        return switch (role) {
            case 2 -> "超级管理员";
            case 1 -> "管理员";
            default -> "普通用户";
        };
    }
}
