package com.example.secondhand_project.pojo.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDTO {


    private String title;

    private String description;

    private BigDecimal price;

    private  Integer category;

    private List<String> images;

}