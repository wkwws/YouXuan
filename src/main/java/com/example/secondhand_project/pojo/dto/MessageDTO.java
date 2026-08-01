package com.example.secondhand_project.pojo.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "发送消息请求参数")
public class MessageDTO {
    @Schema(description = "消息内容", example = "你好，这个商品还在吗？")
    private String content;

    @Schema(description = "消息图片URL（选填）")
    private String image;

}
