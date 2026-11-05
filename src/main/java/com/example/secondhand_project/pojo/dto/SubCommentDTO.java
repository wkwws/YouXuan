package com.example.secondhand_project.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发布子评论 / 回复请求参数")
public class SubCommentDTO {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复内容不能超过500字")
    @Schema(description = "回复内容，最长500字", example = "明天再去")
    private String content;

    @Schema(description = "回复的目标子评论ID；为空表示直接回复一级评论", example = "9001")
    private Long replySubId;

}
