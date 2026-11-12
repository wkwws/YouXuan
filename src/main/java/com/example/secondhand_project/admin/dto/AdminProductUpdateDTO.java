package com.example.secondhand_project.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminProductUpdateDTO {

    private String title;

    private String description;

    private BigDecimal price;

    private Integer category;
}
