package com.example.secondhand_project.admin.service;

import com.example.secondhand_project.admin.entity.AdminAuditTask;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.service.IUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台统计。数据量不大，直接用 JdbcTemplate 做聚合，避免为此再写一堆 Mapper。
 */
@Service
public class AdminDashboardService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("M/d");

    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    private final AdminAuditTaskService auditTaskService;

    private final IUserService userService;

    public AdminDashboardService(JdbcTemplate jdbcTemplate, AdminAuditTaskService auditTaskService,
                                 IUserService userService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditTaskService = auditTaskService;
        this.userService = userService;
    }

    public Map<String, Object> summary() {
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("users", count("SELECT COUNT(*) FROM `user`"));
        totals.put("products", count("SELECT COUNT(*) FROM product WHERE audit_status <> 2"));
        totals.put("posts", count("SELECT COUNT(*) FROM post WHERE audit_status <> 2 AND visible = 1"));
        totals.put("comments", 0);
        totals.put("online", 0);

        Map<String, Object> today = new LinkedHashMap<>();
        today.put("users", count("SELECT COUNT(*) FROM `user` WHERE DATE(created_at) = CURDATE()"));
        today.put("products", count("SELECT COUNT(*) FROM product WHERE audit_status <> 2 AND DATE(created_at) = CURDATE()"));
        today.put("posts", count("SELECT COUNT(*) FROM post WHERE audit_status <> 2 AND visible = 1 AND DATE(created_at) = CURDATE()"));
        today.put("comments", 0);
        today.put("onlineDelta", 0);

        long totalTasks = auditTaskService.total();
        long pass = auditTaskService.countByStatus(1);
        long pending = auditTaskService.countByStatus(0);
        long reject = auditTaskService.countByStatus(2);
        Map<String, Object> moderation = new LinkedHashMap<>();
        moderation.put("pass", percent(pass, totalTasks));
        moderation.put("pending", percent(pending, totalTasks));
        moderation.put("reject", percent(reject, totalTasks));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totals", totals);
        result.put("today", today);
        result.put("moderation", moderation);
        result.put("pendingCount", pending);
        result.put("bannedUsers", count("SELECT COUNT(*) FROM `user` WHERE status = 1"));
        result.put("totalAuditTasks", totalTasks);
        return result;
    }

    public List<Map<String, Object>> trend(int days) {
        int span = Math.min(Math.max(days, 1), 30);
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = span - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", day.format(DAY_LABEL));
            item.put("users", countBetween("user", start, end));
            item.put("products", countBetween("product", start, end));
            item.put("posts", countBetween("post", start, end));
            list.add(item);
        }
        return list;
    }

    public List<Map<String, Object>> categoryDistribution() {
        return jdbcTemplate.query(
                "SELECT c.name AS name, COUNT(p.id) AS value " +
                        "FROM category c LEFT JOIN product p ON p.category_id = c.id AND p.audit_status <> 2 " +
                        "GROUP BY c.id, c.name ORDER BY value DESC",
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", rs.getString("name"));
                    item.put("value", rs.getLong("value"));
                    return item;
                });
    }

    public List<Map<String, Object>> pending(int limit) {
        List<AdminAuditTask> tasks = auditTaskService.latest(limit);
        List<Map<String, Object>> list = new ArrayList<>();
        for (AdminAuditTask task : tasks) {
            User submitter = task.getSubmitterId() == null ? null
                    : userService.getById(task.getSubmitterId());
            String name = submitter == null ? "未知用户"
                    : (submitter.getNickname() == null ? submitter.getUsername() : submitter.getNickname());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", task.getId());
            item.put("type", task.getTargetType() == null ? "" : task.getTargetType().toLowerCase());
            item.put("title", task.getTitle());
            item.put("meta", name + " · " + (task.getCreatedAt() == null
                    ? "" : task.getCreatedAt().format(TIME_LABEL)));
            item.put("tag", "待审核");
            list.add(item);
        }
        return list;
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private long countBetween(String table, LocalDateTime start, LocalDateTime end) {
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `" + table + "` WHERE created_at >= ? AND created_at < ?",
                Long.class, start, end);
        return value == null ? 0 : value;
    }

    private long percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(value * 100.0 / total);
    }
}
