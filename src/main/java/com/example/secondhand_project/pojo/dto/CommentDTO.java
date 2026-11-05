package com.example.secondhand_project.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发布一级评论请求参数")
public class CommentDTO {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500字")
    @Schema(description = "评论内容，最长500字", example = "图书馆确实人多")
    private String content;

}
