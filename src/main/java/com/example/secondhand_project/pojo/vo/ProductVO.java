package com.example.secondhand_project.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@Schema(description = "商品简要信息（列表展示用）")
public class ProductVO {

    @Schema(description = "商品ID")
    private Integer id;

    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "商品价格")
    private BigDecimal price;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "商品图片列表")
    private List<String> images;

}
