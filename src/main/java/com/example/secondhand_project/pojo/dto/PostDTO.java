package com.example.secondhand_project.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "发布说说请求参数")
public class PostDTO {

    @Schema(description = "说说内容", example = "今天天气真好，有人一起去图书馆吗？")
    private String content;

    @Schema(description = "图片URL列表")
    private List<String> images;

}
