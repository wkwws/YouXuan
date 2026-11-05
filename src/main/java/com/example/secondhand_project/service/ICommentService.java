package com.example.secondhand_project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.secondhand_project.pojo.dto.CommentDTO;
import com.example.secondhand_project.pojo.dto.SubCommentDTO;
import com.example.secondhand_project.pojo.entity.Comment;
import com.example.secondhand_project.pojo.vo.CommentVO;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.SubCommentVO;

/**
 * <p>
 * 说说评论服务类
 * </p>
 *
 * @author wkw
 */
public interface ICommentService extends IService<Comment> {

    PageVO<CommentVO> pageComments(int postId, int page, int pageSize);

    CommentVO addComment(int postId, CommentDTO dto);

    PageVO<SubCommentVO> pageReplies(long commentId, int page, int pageSize);

    SubCommentVO addReply(long commentId, SubCommentDTO dto);

    void deleteComment(long commentId);

    void deleteSubComment(long id);

}
