package com.example.secondhand_project.controller;

import com.example.secondhand_project.annotation.RateLimit;
import com.example.secondhand_project.enums.LikeBizTypeEnum;
import com.example.secondhand_project.pojo.dto.LikeRecordFormDTO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.ILikedRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 点赞记录表 控制器（融合自 点赞功能模板/tj-remark）
 * </p>
 *
 * @author wkw
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/likes")
@Tag(name = "06-点赞管理", description = "说说点赞 / 取消点赞")
public class LikedRecordController {

    private final ILikedRecordService likedRecordService;

    @Operation(
            summary = "点赞或取消点赞",
            description = "liked=true 点赞，liked=false 取消点赞；重复操作幂等，返回当前点赞总数"
    )
    @RateLimit(count = 30, window = 60, key = "like:toggle",
            message = "点赞操作过于频繁，请稍后再试")
    @PostMapping
    public Result<Long> addordeleteRecord(@Valid @RequestBody LikeRecordFormDTO likeRecordFormDTO) {
        return Result.success(likedRecordService.addordeleteRecord(likeRecordFormDTO));
    }

    @Operation(summary = "查询当前用户已点赞的业务ID列表", description = "前端拿到列表后用于点亮爱心")
    @GetMapping("/list")
    public Result<List<Long>> showUserLike(
            @Parameter(description = "业务类型：POST=说说，COMMENT=评论", example = "POST")
            @RequestParam(value = "bizType", defaultValue = "POST") LikeBizTypeEnum bizType,
            @Parameter(description = "业务ID列表，逗号分隔", example = "1,2,3")
            @RequestParam("bizIds") List<Long> bizIds) {
        return Result.success(likedRecordService.showUserLike(bizType, bizIds));
    }

}
