package com.example.secondhand_project.admin.dto;

import lombok.Data;

/**
 * 后台通用分页查询参数。
 *
 * <p>前台可能把空字符串传上来，这里统一在 service 里按“空即不过滤”处理，
 * 不要直接把空串拼进 SQL 条件。</p>
 */
@Data
public class AdminPageQuery {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String keyword;

    private String status;

    private String category;

    private String visibility;

    private String targetType;

    private String module;

    public int safePage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    public String trimmedKeyword() {
        return keyword == null ? "" : keyword.trim();
    }

    public boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
