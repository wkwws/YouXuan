package com.example.secondhand_project.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "消息信息")
public class MessageVO {

    @Schema(description = "消息ID")
    private Integer id;

    @Schema(description = "会话ID")
    private Integer conversationId;

    @Schema(description = "发送者用户ID")
    private Integer fromUserId;

    @Schema(description = "接收者用户ID")
    private Integer toUserId;

    @Schema(description = "发送者用户名")
    private String fromUsername;

    @Schema(description = "发送者头像URL")
    private String fromAvatar;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息图片URL")
    private String image;

    @Schema(description = "消息类型", example = "text/image")
    private String msgType;

    @Schema(description = "消息状态（0-未读，1-已读）")
    private Integer status;

    @Schema(description = "发送时间")
    private LocalDateTime createAt;

}
