package com.example.secondhand_project.admin.vo;

import lombok.Data;

import java.util.List;

/**
 * 后台分页返回结构：显式给出 total / pages，避免复用 C 端 PageVO 的歧义。
 */
@Data
public class AdminPageVO<T> {

    private List<T> list;

    private long total;

    private long page;

    private long pageSize;

    private long pages;

    public static <T> AdminPageVO<T> of(List<T> list, long total, long page, long pageSize) {
        AdminPageVO<T> vo = new AdminPageVO<>();
        vo.setList(list);
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        vo.setPages(pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize);
        return vo;
    }
}
