package com.example.secondhand_project.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "子评论信息")
public class SubCommentVO {

    @Schema(description = "子评论ID")
    private Long id;

    @Schema(description = "所属一级评论ID")
    private Long commentId;

    @Schema(description = "回复者用户ID")
    private Integer uid;

    @Schema(description = "回复者昵称")
    private String nickname;

    @Schema(description = "回复者头像")
    private String avatar;

    @Schema(description = "被回复用户ID")
    private Integer replyToUid;

    @Schema(description = "被回复用户昵称")
    private String replyToNickname;

    @Schema(description = "回复内容")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "回复时间")
    private LocalDateTime createdAt;

}
