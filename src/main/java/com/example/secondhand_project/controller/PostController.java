package com.example.secondhand_project.controller;


import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.pojo.dto.PostDTO;
import com.example.secondhand_project.pojo.entity.Post;
import com.example.secondhand_project.pojo.vo.PostVO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.IPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 说说表（校园生活圈） 前端控制器
 * </p>
 *
 * @author wkw
 * @since 2026-05-31
 */
@RestController
@RequestMapping("/posts")
@Tag(name = "05-说说管理（校园生活圈）", description = "校园说说的增删查")
public class PostController {

    @Autowired
    private IPostService iPostService;


    //TODO redis 优化方案
    /**
     * 添加说说
     * @param post
     * @return
     */
    @Operation(summary = "发布说说", description = "发布一条校园生活说说（需登录）")
    @PostMapping()
    public Result addpost(@RequestBody PostDTO post) {

        PostVO addpost = iPostService.addpost(post);

        return Result.success(addpost);
    }


    /**
     * 获取说说列表：无参数，默认最新 10 条
     * @return
     */
    @Operation(summary = "获取说说列表", description = "无参数，一次返回最新 10 条说说")
    @GetMapping
    public Result<List<PostVO>> getpost() {
        return Result.success(iPostService.listLatest());
    }


    /**
     *
     * 删除说说
     * @param id
     * @return
     */
    @Operation(summary = "删除说说", description = "根据 ID 删除说说（需登录，仅自己的说说）")
    @DeleteMapping("/{id}")
    public Result deletepost(@Parameter(description = "说说 ID") @PathVariable int id){

        Post existing = iPostService.getById(id);
        if (existing == null) {
            return Result.error("说说不存在");
        }
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null || !currentId.equals(existing.getUid().longValue())) {
            return Result.error("无权删除该说说");
        }
        iPostService.deletepost(id);

        return Result.success("删除成功");
    }


}
