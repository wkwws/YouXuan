package com.example.secondhand_project.controller; // 控制器所在的包


import com.example.secondhand_project.annotation.RateLimit; // 接口限流注解
import com.example.secondhand_project.conf.FeedProperties; // 首页说说条数等 Feed 配置
import com.example.secondhand_project.context.BaseContext; // 当前登录用户 ID
import com.example.secondhand_project.pojo.dto.PostDTO; // 发布说说的请求体
import com.example.secondhand_project.pojo.entity.Post; // 说说实体
import com.example.secondhand_project.pojo.vo.PostVO; // 返回给前端的说说
import com.example.secondhand_project.result.Result; // 统一返回格式
import com.example.secondhand_project.service.IFeedService; // 随机 Feed 服务
import com.example.secondhand_project.service.IPostService; // 说说基础服务
import io.swagger.v3.oas.annotations.Operation; // Swagger 接口说明
import io.swagger.v3.oas.annotations.Parameter; // Swagger 参数说明
import io.swagger.v3.oas.annotations.tags.Tag; // Swagger 分组
import org.springframework.beans.factory.annotation.Autowired; // 自动注入
import org.springframework.web.bind.annotation.*; // Spring MVC 注解

import java.util.Arrays; // 拆分 exclude 字符串
import java.util.Collections; // 返回空列表
import java.util.List; // 返回多条说说
import java.util.Objects; // 过滤空值
import java.util.stream.Collectors; // 把流转成 List

/**
 * <p>
 * 说说表（校园生活圈） 前端控制器
 * </p>
 *
 * @author wkw
 * @since 2026-05-31
 */
@RestController // 返回 JSON
@RequestMapping("/posts") // 所有接口以 /posts 开头
@Tag(name = "05-说说管理（校园生活圈）", description = "校园说说的增删查") // Swagger 分组
public class PostController {

    @Autowired // 注入说说服务
    private IPostService iPostService;

    @Autowired // 注入随机 Feed 服务
    private IFeedService feedService;

    @Autowired // 注入 Feed 配置，用于读取首页默认说说条数
    private FeedProperties feedProperties;


    /**
     * 添加说说
     *
     * @param post 说说内容
     */
    @Operation(summary = "发布说说", description = "发布一条校园生活说说（需登录）") // Swagger 说明
    @RateLimit(count = 5, window = 60, key = "post:create",
            message = "发帖过于频繁，请稍后再试") // 同一用户 60 秒内最多发 5 条
    @PostMapping() // POST /posts
    public Result addpost(@RequestBody PostDTO post) { // 前端传来的 content/images

        PostVO addpost = iPostService.addpost(post); // 真正保存说说；内部会失效随机池

        return Result.success(addpost); // 返回新说说
    }


    /**
     * 按 ID 获取单条说说，供通知点击后打开详情。
     */
    @Operation(summary = "获取单条说说详情") // Swagger 接口说明
    @GetMapping("/{id}") // 处理 GET /posts/{id}
    public Result<PostVO> getPost(@Parameter(description = "说说 ID") @PathVariable int id) { // 路径中的说说 ID
        PostVO post = feedService.getPost(id); // 查询可见的说说详情
        if (post == null) { // 说说不存在、被隐藏或审核未通过
            return Result.error("说说不存在或不可见"); // 返回业务错误
        }
        return Result.success(post); // 返回说说详情
    }

    /**
     * 随机获取说说：默认条数由 feed.size 配置，可选 size 和 exclude。
     *
     * <p>exclude 传上一批的 ID，用于“换一批”尽量不重复。</p>
     */
    @Operation(summary = "随机获取说说列表",
            description = "按时间 + 点赞加权随机返回，默认条数由 feed.size 配置；exclude 传上一批 ID 可减少重复")
    @GetMapping // GET /posts
    public Result<List<PostVO>> getpost(
            @Parameter(description = "返回条数，不传时使用 feed.size 配置；上限由 feed.max-size 控制")
            @RequestParam(value = "size", required = false) Integer size, // 前端要几条，不传用配置默认值
            @Parameter(description = "上一批说说 ID，逗号分隔")
            @RequestParam(value = "exclude", required = false) String exclude) { // 上一批的 ID
        int limit = size == null ? feedProperties.getSize() : size; // 未指定时读取 feed.size
        return Result.success(feedService.randomFeed(limit, parseExclude(exclude))); // 随机取一页并返回
    }

    /**
     * 把 "1,2,3" 解析成 [1,2,3]。
     */
    private List<Integer> parseExclude(String exclude) { // 解析前端传来的上一批 ID
        if (exclude == null || exclude.isBlank()) { // 没传或空字符串
            return Collections.emptyList(); // 返回空列表
        }
        return Arrays.stream(exclude.split(",")) // 按逗号拆开
                .map(String::trim) // 去掉每个 ID 两边的空格
                .filter(value -> !value.isEmpty()) // 过滤空字符串
                .limit(50) // 最多处理 50 个，防止参数太长
                .map(value -> { // 把字符串转成整数
                    try { // 正常数字
                        return Integer.valueOf(value); // 转成 Integer
                    } catch (NumberFormatException e) { // 不是数字
                        return null; // 先标记成 null
                    }
                })
                .filter(Objects::nonNull) // 丢掉不是数字的项
                .collect(Collectors.toList()); // 收集成 List
    }


    /**
     * 删除说说
     */
    @Operation(summary = "删除说说", description = "根据 ID 删除说说（需登录，仅自己的说说）") // Swagger 说明
    @RateLimit(count = 10, window = 60, key = "post:delete",
            message = "操作过于频繁，请稍后再试") // 同一用户 60 秒内最多删 10 条
    @DeleteMapping("/{id}") // DELETE /posts/{id}
    public Result deletepost(@Parameter(description = "说说 ID") @PathVariable int id) { // 路径里的说说 ID

        Post existing = iPostService.getById(id); // 先查这条说说在不在
        if (existing == null) { // 不存在
            return Result.error("说说不存在"); // 返回错误
        }
        Long currentId = BaseContext.getCurrentId(); // 当前登录用户
        if (currentId == null || !currentId.equals(existing.getUid().longValue())) { // 没登录或不是作者
            return Result.error("无权删除该说说"); // 返回无权限
        }
        iPostService.deletepost(id); // 真正删除；内部会在事务提交后失效缓存

        return Result.success("删除成功"); // 返回成功
    }


}
