package com.example.secondhand_project.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 某个业务对象的点赞总数（对应模板 tj-remark 的 LikedTimesDTO）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "业务对象的点赞总数")
public class LikedTimesDTO {

    /**
     * 点赞的业务id
     */
    @Schema(description = "业务ID，例如说说ID")
    private Long bizId;

    /**
     * 总的点赞次数
     */
    @Schema(description = "总点赞次数")
    private Integer likedTimes;

}
