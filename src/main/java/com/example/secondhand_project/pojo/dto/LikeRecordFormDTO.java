package com.example.secondhand_project.pojo.dto;

import com.example.secondhand_project.enums.LikeBizTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 点赞请求表单（对应模板 tj-remark 的 LikeRecordFormDTO）
 */
@Data
@Schema(description = "点赞记录表单")
public class LikeRecordFormDTO {

    @NotNull(message = "业务id不能为空")
    @Schema(description = "点赞业务ID，例如说说ID", example = "12")
    private Long bizId;

    @NotNull(message = "业务类型不能为空")
    @Schema(description = "业务类型：POST=说说，COMMENT=评论", example = "POST")
    private LikeBizTypeEnum bizType;

    @NotNull(message = "是否点赞不能为空")
    @Schema(description = "true=点赞；false=取消点赞", example = "true")
    private Boolean liked;

}
