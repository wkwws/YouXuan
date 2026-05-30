package com.example.secondhand_project.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 文件上传记录表
 * </p>
 *
 * @author wkw
 * @since 2026-05-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("upload")
public class Upload implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件ID，主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 存储文件名（UUID 重命名）
     */
    private String filename;

    /**
     * 原始文件名
     */
    private String originName;

    /**
     * 文件访问路径
     */
    private String url;

    /**
     * MIME 类型，如 image/jpeg
     */
    private String mime;

    /**
     * 文件大小（字节）
     */
    private Integer fsize;

    /**
     * 上传者用户ID
     */
    private Integer uid;

    /**
     * 上传时间
     */
    private LocalDateTime createdAt;


}
