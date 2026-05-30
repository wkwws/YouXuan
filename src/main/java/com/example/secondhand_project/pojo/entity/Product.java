package com.example.secondhand_project.pojo.entity;

import java.math.BigDecimal;

import cn.hutool.json.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.List;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 商品表
 * </p>
 *
 * @author wkw
 * @since 2026-05-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "product",autoResultMap = true)
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID，主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 商品描述
     */
    @TableField("`desc`")
    private String description;

    /**
     * 价格（元）
     */
    private BigDecimal price;

    /**
     * 分类ID，关联 category.id
     */
    @TableField("category_id")
    private Integer category;
    /**
     * 发布者用户ID，关联 user.id
     */
    private Integer uid;

    /**
     * 图片URL数组，如 ["url1","url2"]
     */
    //                          这个是将object转换成json与数据库进行对照
    @TableField(value = "imgs",typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    /**
     * 商品状态：0-在售 1-已售出 2-下架
     */
    private Integer status;

    /**
     * 发布时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;


}
