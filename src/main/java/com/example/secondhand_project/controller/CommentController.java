package com.example.secondhand_project.controller;

import com.example.secondhand_project.pojo.dto.CommentDTO;
import com.example.secondhand_project.pojo.dto.SubCommentDTO;
import com.example.secondhand_project.pojo.vo.CommentVO;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.SubCommentVO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.ICommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 说说评论 / 子评论控制器
 * </p>
 *
 * @author wkw
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "07-评论管理", description = "说说一级评论、子评论、删除")
public class CommentController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ICommentService commentService;

    @Operation(summary = "分页查询说说评论", description = "每条评论附带前3条子评论预览，未登录时 liked 为 false")
    @GetMapping("/posts/{postId}/comments")
    public Result<PageVO<CommentVO>> pageComments(
            @PathVariable int postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(commentService.pageComments(postId, page, safeSize(pageSize)));
    }

    @Operation(summary = "发布一级评论", description = "需登录，内容最长500字")
    @PostMapping("/posts/{postId}/comments")
    public Result<CommentVO> addComment(
            @PathVariable int postId,
            @Valid @RequestBody CommentDTO dto) {
        return Result.success(commentService.addComment(postId, dto));
    }

    @Operation(summary = "分页查询子评论", description = "用于“查看全部 N 条回复”")
    @GetMapping("/comments/{commentId}/replies")
    public Result<PageVO<SubCommentVO>> pageReplies(
            @PathVariable long commentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(commentService.pageReplies(commentId, page, safeSize(pageSize)));
    }

    @Operation(summary = "发布子评论 / 回复", description = "需登录，可选 replySubId 指定回复哪条子评论")
    @PostMapping("/comments/{commentId}/replies")
    public Result<SubCommentVO> addReply(
            @PathVariable long commentId,
            @Valid @RequestBody SubCommentDTO dto) {
        return Result.success(commentService.addReply(commentId, dto));
    }

    @Operation(summary = "删除一级评论", description = "评论作者或说说作者可删；子评论级联软删除")
    @DeleteMapping("/comments/{commentId}")
    public Result<String> deleteComment(
            @Parameter(description = "评论ID") @PathVariable long commentId) {
        commentService.deleteComment(commentId);
        return Result.success("删除成功");
    }

    @Operation(summary = "删除子评论", description = "回复作者或说说作者可删")
    @DeleteMapping("/sub-comments/{id}")
    public Result<String> deleteSubComment(
            @Parameter(description = "子评论ID") @PathVariable long id) {
        commentService.deleteSubComment(id);
        return Result.success("删除成功");
    }

    private int safeSize(int pageSize) {
        if (pageSize <= 0) {
            return 20;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

}
