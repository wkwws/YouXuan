package com.example.secondhand_project.pojo.vo;


import com.example.secondhand_project.pojo.entity.LastMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "私信会话信息")
public class ConversationVO {

    @Schema(description = "会话ID")
    private Integer conversationId;

    @Schema(description = "对方用户ID")
    private Integer userId;

    @Schema(description = "对方用户名")
    private String username;

    @Schema(description = "对方昵称")
    private String nickname;

    @Schema(description = "对方头像URL")
    private String avatar;

    @Schema(description = "最后一条消息内容")
    private String lastMessage;

    @Schema(description = "未读消息数")
    private Integer unread;

    @Schema(description = "最后消息时间")
    private LocalDateTime lastTime;

}
