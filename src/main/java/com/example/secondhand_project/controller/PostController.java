package com.example.secondhand_project.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.secondhand_project.pojo.dto.PostDTO;
import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.entity.Post;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.PostVO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.IPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


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
     * 获取说说列表
     * @return
     */
    @Operation(summary = "获取说说列表", description = "分页获取所有校园说说")
    @GetMapping
    public Result getpost(Pagee pagee){

        PageVO<PostVO> po= iPostService.getpost(pagee);

        return Result.success(po);
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

        //直接调用service层中的方法

        iPostService.deletepost(id);


        return Result.success("删除成功");
    }


}
