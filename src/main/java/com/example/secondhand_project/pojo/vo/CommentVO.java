package com.example.secondhand_project.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "一级评论信息")
public class CommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "所属说说ID")
    private Integer postId;

    @Schema(description = "评论用户ID")
    private Integer uid;

    @Schema(description = "评论用户昵称")
    private String nickname;

    @Schema(description = "评论用户头像")
    private String avatar;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论点赞数")
    private Integer likeCount;

    @Schema(description = "当前用户是否已赞")
    private Boolean liked;

    @Schema(description = "子评论总数")
    private Integer replyCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "评论时间")
    private LocalDateTime createdAt;

    @Schema(description = "子评论预览（前3条）")
    private List<SubCommentVO> subComments;

}
