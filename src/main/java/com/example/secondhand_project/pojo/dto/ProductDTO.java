package com.example.secondhand_project.pojo.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "添加/更新商品请求参数")
public class ProductDTO {

    @Schema(description = "商品标题", example = "二手iPhone 14 Pro Max 99新")
    private String title;

    @Schema(description = "商品描述", example = "自用一手，无划痕，电池健康90%")
    private String description;

    @Schema(description = "商品价格", example = "5999.00")
    private BigDecimal price;

    @Schema(description = "分类ID", example = "1")
    private Integer category;

    @Schema(description = "商品图片URL列表")
    private List<String> images;

}