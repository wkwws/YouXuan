package com.example.secondhand_project.pojo.vo;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("category")
public class CategoryVO {

    private Integer id;

    private String name;

    private String val;

    private String icon;

    private long sortOrder;


}
