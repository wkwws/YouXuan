package com.example.secondhand_project.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.secondhand_project.admin.context.AdminContext;
import com.example.secondhand_project.admin.dto.AdminPageQuery;
import com.example.secondhand_project.admin.vo.AdminPageVO;
import com.example.secondhand_project.pojo.entity.Post;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.service.IFeedPoolService;
import com.example.secondhand_project.service.IPostService;
import com.example.secondhand_project.service.IUserService;
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
 * 管理端说说 CRUD + 基础审核/隐藏。
 */
@Service
public class AdminPostService {

    private final IPostService postService;

    private final IUserService userService;

    private final AdminAuditTaskService auditTaskService;

    private final AdminLogService logService;

    private final IFeedPoolService feedPoolService;

    public AdminPostService(IPostService postService, IUserService userService,
                            AdminAuditTaskService auditTaskService, AdminLogService logService,
                            IFeedPoolService feedPoolService) {
        this.postService = postService;
        this.userService = userService;
        this.auditTaskService = auditTaskService;
        this.logService = logService;
        this.feedPoolService = feedPoolService;
    }

    public AdminPageVO<Map<String, Object>> page(AdminPageQuery query) {
        int page = query.safePage();
        int pageSize = query.safePageSize();
        String keyword = query.trimmedKeyword();
        String visibility = query.getVisibility() == null ? "" : query.getVisibility().trim();
        String status = query.getStatus() == null ? "" : query.getStatus().trim();

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        if ("visible".equals(visibility)) {
            wrapper.eq(Post::getVisible, 1);
        } else if ("hidden".equals(visibility)) {
            wrapper.eq(Post::getVisible, 0);
        }
        if ("pending".equals(status)) {
            wrapper.eq(Post::getAuditStatus, 0);
        } else if ("approved".equals(status)) {
            wrapper.eq(Post::getAuditStatus, 1);
        } else if ("rejected".equals(status)) {
            wrapper.eq(Post::getAuditStatus, 2);
        }

        if (!keyword.isEmpty()) {
            List<Integer> authorIds = userService.lambdaQuery()
                    .and(w -> w.like(User::getNickname, keyword).or()
                            .like(User::getUsername, keyword))
                    .list()
                    .stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            wrapper.and(w -> {
                w.like(Post::getContent, keyword);
                if (!authorIds.isEmpty()) {
                    w.or().in(Post::getUid, authorIds);
                }
            });
        }

        wrapper.orderByDesc(Post::getCreatedAt);
        Page<Post> result = postService.page(new Page<>(page, pageSize), wrapper);
        return AdminPageVO.of(toVOList(result.getRecords()), result.getTotal(), page, pageSize);
    }

    public Map<String, Object> detail(Integer id) {
        Post post = postService.getById(id);
        if (post == null) {
            throw new IllegalArgumentException("说说不存在");
        }
        User author = post.getUid() == null ? null : userService.getById(post.getUid());
        return toVO(post, author);
    }

    public void hide(Integer id, String reason) {
        Post post = requirePost(id);
        post.setVisible(0)
                .setHiddenReason(requireReason(reason, "隐藏原因"))
                .setUpdatedAt(LocalDateTime.now());
        postService.updateById(post);
        refreshFeed(id);
        logService.record("说说管理", "隐藏说说", "说说 #" + id);
    }

    public void unhide(Integer id) {
        Post post = requirePost(id);
        post.setVisible(1)
                .setHiddenReason("")
                .setUpdatedAt(LocalDateTime.now());
        postService.updateById(post);
        refreshFeed(id);
        logService.record("说说管理", "恢复说说", "说说 #" + id);
    }

    @Transactional
    public void delete(Integer id) {
        Post post = requirePost(id);
        postService.deletepost(id);
        auditTaskService.remove("POST", id);
        refreshFeed(id);
        logService.record("说说管理", "删除说说", "说说 #" + id);
    }

    @Transactional
    public void audit(Integer id, String action, String reason) {
        Post post = requirePost(id);
        boolean approve = "approve".equalsIgnoreCase(action) || "pass".equalsIgnoreCase(action);
        String remark = approve ? "" : requireReason(reason, "驳回原因");
        post.setAuditedBy(AdminContext.getId())
                .setAuditedAt(LocalDateTime.now())
                .setAuditRemark(remark)
                .setUpdatedAt(LocalDateTime.now());
        if (approve) {
            post.setAuditStatus(1).setVisible(1);
        } else {
            post.setAuditStatus(2).setVisible(0).setHiddenReason(remark);
        }
        postService.updateById(post);
        auditTaskService.mark("POST", id, approve ? 1 : 2, remark, AdminContext.getId());
        refreshFeed(id);
        logService.record("内容审核", approve ? "通过说说" : "驳回说说", "说说 #" + id);
    }

    private Post requirePost(Integer id) {
        Post post = postService.getById(id);
        if (post == null) {
            throw new IllegalArgumentException("说说不存在");
        }
        return post;
    }

    private String requireReason(String reason, String label) {
        if (reason == null || reason.trim().length() < 2) {
            throw new IllegalArgumentException("请填写至少 2 个字的" + label);
        }
        return reason.trim();
    }

    private void refreshFeed(Integer id) {
        try {
            feedPoolService.evictPost(id);
            feedPoolService.rebuild();
        } catch (Exception ignored) {
            // 缓存失效失败不影响审核结果
        }
    }

    private List<Map<String, Object>> toVOList(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Integer> uids = posts.stream()
                .map(Post::getUid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, User> users = uids.isEmpty() ? Collections.emptyMap()
                : userService.listByIds(uids).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        return posts.stream()
                .map(post -> toVO(post, users.get(post.getUid())))
                .collect(Collectors.toList());
    }

    private Map<String, Object> toVO(Post post, User author) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("content", post.getContent());
        map.put("authorId", post.getUid());
        map.put("author", publicUser(author));
        map.put("authorName", author == null ? "未知用户"
                : (author.getNickname() == null ? author.getUsername() : author.getNickname()));
        int imageCount = post.getImages() == null ? 0 : post.getImages().size();
        map.put("images", imageCount);
        map.put("imageUrls", post.getImages() == null ? Collections.emptyList() : post.getImages());
        map.put("likes", post.getLikeCount() == null ? 0 : post.getLikeCount());
        map.put("comments", post.getCommentCount() == null ? 0 : post.getCommentCount());
        map.put("visibility", post.getVisible() != null && post.getVisible() == 0 ? "hidden" : "visible");
        map.put("hiddenReason", post.getHiddenReason());
        map.put("auditStatus", post.getAuditStatus());
        map.put("auditStatusText", auditStatusText(post.getAuditStatus()));
        map.put("auditRemark", post.getAuditRemark());
        map.put("createdAt", post.getCreatedAt());
        map.put("updatedAt", post.getUpdatedAt());
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
}
