package com.example.secondhand_project.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secondhand_project.admin.dto.AdminPageQuery;
import com.example.secondhand_project.admin.entity.AdminAuditTask;
import com.example.secondhand_project.admin.mapper.AdminAuditTaskMapper;
import com.example.secondhand_project.admin.vo.AdminPageVO;
import com.example.secondhand_project.mapper.UserMapper;
import com.example.secondhand_project.pojo.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 待审核 UGC 队列。只负责队列本身，不直接改 product/post。
 */
@Service
public class AdminAuditTaskService extends ServiceImpl<AdminAuditTaskMapper, AdminAuditTask> {

    private final UserMapper userMapper;

    private final ObjectMapper objectMapper;

    public AdminAuditTaskService(UserMapper userMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 内容发布/编辑后进入待审队列；同一目标只保留一条任务。
     */
    @Transactional
    public void submit(String targetType, Integer targetId, Integer submitterId,
                       String title, String content, String snapshotJson) {
        AdminAuditTask existing = lambdaQuery()
                .eq(AdminAuditTask::getTargetType, targetType)
                .eq(AdminAuditTask::getTargetId, targetId)
                .one();
        if (existing != null) {
            existing.setSubmitterId(submitterId)
                    .setTitle(truncate(title, 255))
                    .setContent(content)
                    .setSnapshotJson(snapshotJson)
                    .setStatus(0)
                    .setReason("")
                    .setReviewerId(null)
                    .setReviewedAt(null)
                    .setCreatedAt(LocalDateTime.now());
            this.updateById(existing);
            return;
        }
        AdminAuditTask task = new AdminAuditTask()
                .setTargetType(targetType)
                .setTargetId(targetId)
                .setSubmitterId(submitterId)
                .setTitle(truncate(title, 255))
                .setContent(content)
                .setSnapshotJson(snapshotJson)
                .setStatus(0)
                .setReason("")
                .setCreatedAt(LocalDateTime.now());
        this.save(task);
    }

    public void remove(String targetType, Integer targetId) {
        lambdaUpdate()
                .eq(AdminAuditTask::getTargetType, targetType)
                .eq(AdminAuditTask::getTargetId, targetId)
                .remove();
    }

    public void mark(String targetType, Integer targetId, int status, String reason, Integer reviewerId) {
        lambdaUpdate()
                .eq(AdminAuditTask::getTargetType, targetType)
                .eq(AdminAuditTask::getTargetId, targetId)
                .set(AdminAuditTask::getStatus, status)
                .set(AdminAuditTask::getReason, reason == null ? "" : reason)
                .set(AdminAuditTask::getReviewerId, reviewerId)
                .set(AdminAuditTask::getReviewedAt, LocalDateTime.now())
                .update();
    }

    public AdminPageVO<Map<String, Object>> page(AdminPageQuery query) {
        int page = query.safePage();
        int pageSize = query.safePageSize();
        String keyword = query.trimmedKeyword();
        Integer status = parseStatus(query.getStatus());
        String targetType = query.getTargetType() == null ? "" : query.getTargetType().trim().toUpperCase();

        var wrapper = lambdaQuery()
                .eq(status != null, AdminAuditTask::getStatus, status)
                .eq(!targetType.isBlank(), AdminAuditTask::getTargetType, targetType)
                .and(query.hasText(keyword), w -> w
                        .like(AdminAuditTask::getTitle, keyword).or()
                        .like(AdminAuditTask::getContent, keyword))
                .orderByDesc(AdminAuditTask::getCreatedAt);

        Page<AdminAuditTask> result = wrapper.page(new Page<>(page, pageSize));
        List<Map<String, Object>> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return AdminPageVO.of(list, result.getTotal(), page, pageSize);
    }

    public Map<String, Object> detail(Long id) {
        AdminAuditTask task = this.getById(id);
        if (task == null) {
            throw new IllegalArgumentException("审核任务不存在");
        }
        return toVO(task);
    }

    public AdminAuditTask getByTarget(String targetType, Integer targetId) {
        return lambdaQuery()
                .eq(AdminAuditTask::getTargetType, targetType)
                .eq(AdminAuditTask::getTargetId, targetId)
                .one();
    }

    public long countByStatus(int status) {
        return lambdaQuery().eq(AdminAuditTask::getStatus, status).count();
    }

    public long total() {
        return this.count();
    }

    public List<AdminAuditTask> latest(int limit) {
        return lambdaQuery()
                .eq(AdminAuditTask::getStatus, 0)
                .orderByDesc(AdminAuditTask::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50)))
                .list();
    }

    public Map<String, Object> toVO(AdminAuditTask task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", task.getId());
        map.put("targetType", task.getTargetType());
        map.put("type", task.getTargetType() == null ? "" : task.getTargetType().toLowerCase());
        map.put("targetId", task.getTargetId());
        map.put("title", task.getTitle());
        map.put("content", task.getContent());
        map.put("submitterId", task.getSubmitterId());
        map.put("submitter", publicUser(task.getSubmitterId()));
        map.put("status", statusText(task.getStatus()));
        map.put("statusCode", task.getStatus());
        map.put("reason", task.getReason());
        map.put("createdAt", task.getCreatedAt());
        map.put("reviewedAt", task.getReviewedAt());
        map.put("reviewerId", task.getReviewerId());
        map.put("reviewerName", nickname(task.getReviewerId()));
        map.put("snapshot", parseSnapshot(task.getSnapshotJson()));
        return map;
    }

    private Map<String, Object> publicUser(Integer uid) {
        User user = uid == null ? null : userMapper.selectById(uid);
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

    private String nickname(Integer uid) {
        User user = uid == null ? null : userMapper.selectById(uid);
        return user == null ? "" : (user.getNickname() == null ? user.getUsername() : user.getNickname());
    }

    private String avatarText(User user) {
        String source = user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername() : user.getNickname();
        return source == null || source.isBlank() ? "?" : source.substring(0, 1);
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "pending";
        }
        return switch (status) {
            case 1 -> "approved";
            case 2 -> "rejected";
            default -> "pending";
        };
    }

    private Integer parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.trim().toLowerCase()) {
            case "pending", "0" -> 0;
            case "approved", "1" -> 1;
            case "rejected", "2" -> 2;
            default -> null;
        };
    }

    private Object parseSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
