package com.example.secondhand_project.pojo.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分页查询参数")
public class Pagee {

    @Schema(description = "页码，从1开始", example = "1")
    private Integer page;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize;

    @Schema(description = "分类ID（筛选条件，选填）", example = "1")
    private Integer category;

    @Schema(description = "关键词搜索（选填）", example = "iPhone")
    private String keyword;

}
