package com.example.secondhand_project.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
public class ProductVO {

    private Integer id;

    private String title;

    private BigDecimal price;

    private LocalDateTime createdAt;

}
