package com.example.secondhand_project.pojo.vo;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "说说信息（校园生活圈）")
public class PostVO {

    @Schema(description = "说说ID")
    private Integer id;

    @Schema(description = "发布者用户ID")
    private Integer uid;

    @Schema(description = "发布者用户名")
    private String username;

    @Schema(description = "发布者昵称")
    private String nickname;

    @Schema(description = "发布者头像URL")
    private String avatar;

    @Schema(description = "说说内容")
    private String content;

    @Schema(description = "图片URL列表")
    private List<String> images;

    @Schema(description = "点赞总数")
    private Integer likeCount;

    @Schema(description = "评论总数（含子评论）")
    private Integer commentCount;

    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发布时间")
    private LocalDateTime createdAt;



}
