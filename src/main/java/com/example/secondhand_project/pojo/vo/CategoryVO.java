package com.example.secondhand_project.pojo.vo;


import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("category")
@Schema(description = "商品分类信息")
public class CategoryVO {

    @Schema(description = "分类ID")
    private Integer id;

    @Schema(description = "分类名称", example = "数码产品")
    private String name;

    @Schema(description = "分类标识值", example = "digital")
    private String val;

    @Schema(description = "分类图标URL")
    private String icon;

    @Schema(description = "排序序号")
    private long sortOrder;


}
