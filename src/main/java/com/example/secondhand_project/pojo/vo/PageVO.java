package com.example.secondhand_project.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 输出分页数据
 * @param <T>
 */
@Data
@Schema(description = "分页返回结果")
public class PageVO<T> {
    @Schema(description = "当前页数据列表")
    private List<T> list;

    @Schema(description = "总记录数")
    private Integer total;

    @Schema(description = "当前页码")
    private Integer page;

    @Schema(description = "每页条数")
    private Integer pageSize;
}
