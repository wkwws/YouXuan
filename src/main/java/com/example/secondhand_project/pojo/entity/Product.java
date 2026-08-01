package com.example.secondhand_project.pojo.entity;

import java.math.BigDecimal;

import cn.hutool.json.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
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
@Schema(description = "商品实体")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID，主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "商品ID")
    private Integer id;

    /**
     * 商品标题
     */
    @Schema(description = "商品标题", example = "二手iPhone 14 Pro Max")
    private String title;

    /**
     * 商品描述
     */
    @TableField("`desc`")
    @Schema(description = "商品描述")
    private String description;

    /**
     * 价格（元）
     */
    @Schema(description = "价格（元）", example = "5999.00")
    private BigDecimal price;

    /**
     * 分类ID，关联 category.id
     */
    @TableField("category_id")
    @Schema(description = "分类ID")
    private Integer category;
    /**
     * 发布者用户ID，关联 user.id
     */
    @Schema(description = "发布者用户ID")
    private Integer uid;

    /**
     * 图片URL数组，如 ["url1","url2"]
     */
    // 这个是将object转换成json与数据库进行对照 最好是用传统的类型，不要用依赖中的类型，否则mp的jackson无法转换成功
    @TableField(value = "imgs",typeHandler = JacksonTypeHandler.class)
    @Schema(description = "商品图片URL列表")
    private List<String> images;

    /**
     * 商品状态：0-在售 1-已售出 2-下架
     */
    @Schema(description = "商品状态：0-在售 1-已售出 2-下架")
    private Integer status;

    /**
     * 发布时间
     */
    @Schema(description = "发布时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 用户名
     */
    @TableField(exist = false)
    @Schema(description = "发布者用户名")
    private String userName;

    @TableField(exist = false)
    @Schema(description = "发布者昵称")
    private String nickName;

}
