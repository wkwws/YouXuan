package com.example.secondhand_project.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.enums.LikeBizTypeEnum;
import com.example.secondhand_project.exception.BusinessException;
import com.example.secondhand_project.exception.LoginException;
import com.example.secondhand_project.mapper.CommentMapper;
import com.example.secondhand_project.mapper.SubCommentMapper;
import com.example.secondhand_project.pojo.dto.CommentDTO;
import com.example.secondhand_project.pojo.dto.SubCommentDTO;
import com.example.secondhand_project.pojo.entity.Comment;
import com.example.secondhand_project.pojo.entity.Post;
import com.example.secondhand_project.pojo.entity.SubComment;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.pojo.vo.CommentVO;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.SubCommentVO;
import com.example.secondhand_project.service.ICommentService;
import com.example.secondhand_project.service.ILikedRecordService;
import com.example.secondhand_project.service.INotificationService; // 评论/回复成功后生成互动通知
import com.example.secondhand_project.service.IPostService;
import com.example.secondhand_project.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization; // 事务回调接口
import org.springframework.transaction.support.TransactionSynchronizationManager; // 事务同步管理器，用来注册“提交后执行”

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 说说评论服务实现类
 * </p>
 *
 * @author wkw
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {

    private final SubCommentMapper subCommentMapper;

    private final IPostService postService;

    private final IUserService userService;

    private final ILikedRecordService likedRecordService;

    private final INotificationService notificationService; // 负责落库并通过 WebSocket 推送互动通知

    @Override
    public PageVO<CommentVO> pageComments(int postId, int page, int pageSize) {
        Page<Comment> p = this.lambdaQuery()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, 1)
                .orderByDesc(Comment::getCreatedAt)
                .page(new Page<>(page, pageSize));

        List<Comment> records = p.getRecords();
        PageVO<CommentVO> result = new PageVO<>();
        result.setTotal((int) p.getTotal());
        result.setPage(page);
        result.setPageSize(pageSize);
        if (records.isEmpty()) {
            result.setList(Collections.emptyList());
            return result;
        }

        List<Long> commentIds = records.stream().map(Comment::getId).collect(Collectors.toList());

        // 一次性取这些评论下所有有效子评论，按时间倒序，再在内存里取每条的前 3 条
        List<SubComment> allSubs = subCommentMapper.selectList(
                new LambdaQueryWrapper<SubComment>()
                        .in(SubComment::getCommentId, commentIds)
                        .eq(SubComment::getStatus, 1)
                        .orderByDesc(SubComment::getCreatedAt)
        );
        Map<Long, List<SubComment>> subMap = allSubs.stream()
                .collect(Collectors.groupingBy(SubComment::getCommentId));

        Set<Integer> userIds = new HashSet<>();
        records.forEach(c -> userIds.add(c.getUid()));
        allSubs.forEach(s -> {
            userIds.add(s.getUid());
            if (s.getReplyUid() != null) {
                userIds.add(s.getReplyUid());
            }
        });
        Map<Integer, User> userMap = loadUsers(userIds);

        // 当前用户对这批评论的点赞状态
        Set<Long> likedSet = new HashSet<>();
        if (BaseContext.getCurrentId() != null) {
            likedSet.addAll(likedRecordService.showUserLike(LikeBizTypeEnum.COMMENT, commentIds));
        }

        List<CommentVO> list = records.stream().map(c -> {
            List<SubComment> subs = subMap.getOrDefault(c.getId(), Collections.emptyList());
            List<SubComment> preview = subs.stream().limit(3).collect(Collectors.toList());
            // 查询是倒序，展示时反转成正序，回复读起来更自然
            Collections.reverse(preview);
            List<SubCommentVO> subVos = preview.stream()
                    .map(s -> toSubCommentVO(s, userMap))
                    .collect(Collectors.toList());

            User author = userMap.get(c.getUid());
            return CommentVO.builder()
                    .id(c.getId())
                    .postId(c.getPostId())
                    .uid(c.getUid())
                    .nickname(displayName(author))
                    .avatar(author == null ? "" : author.getAvatar())
                    .content(c.getContent())
                    .likeCount(c.getLikeCount() == null ? 0 : c.getLikeCount())
                    .liked(likedSet.contains(c.getId()))
                    .replyCount(c.getReplyCount() == null ? 0 : c.getReplyCount())
                    .createdAt(c.getCreatedAt())
                    .subComments(subVos)
                    .build();
        }).collect(Collectors.toList());

        result.setList(list);
        return result;
    }

    @Override
    @Transactional
    public CommentVO addComment(int postId, CommentDTO dto) {
        Long uid = currentUid();
        Post post = postService.getById(postId);
        if (post == null) {
            throw new BusinessException("说说不存在");
        }

        String content = normalizeContent(dto.getContent());
        Comment comment = new Comment()
                .setPostId(postId)
                .setUid(uid.intValue())
                .setContent(content)
                .setLikeCount(0)
                .setReplyCount(0)
                .setStatus(1)
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());
        this.save(comment);

        postService.lambdaUpdate()
                .eq(Post::getId, postId)
                .setSql("comment_count = comment_count + 1")
                .update();
        evictFeedPostAfterCommit(postId); // 评论数变了，事务提交后删这条帖子的快照
        notificationService.notifyNewComment(post, comment); // 通知说说作者（自己评论自己不通知）

        User author = userService.getById(uid.intValue());
        return CommentVO.builder()
                .id(comment.getId())
                .postId(postId)
                .uid(uid.intValue())
                .nickname(displayName(author))
                .avatar(author == null ? "" : author.getAvatar())
                .content(content)
                .likeCount(0)
                .liked(false)
                .replyCount(0)
                .createdAt(comment.getCreatedAt())
                .subComments(Collections.emptyList())
                .build();
    }

    @Override
    public PageVO<SubCommentVO> pageReplies(long commentId, int page, int pageSize) {
        Page<SubComment> p = subCommentMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<SubComment>()
                        .eq(SubComment::getCommentId, commentId)
                        .eq(SubComment::getStatus, 1)
                        .orderByAsc(SubComment::getCreatedAt)
        );

        List<SubComment> records = p.getRecords();
        PageVO<SubCommentVO> result = new PageVO<>();
        result.setTotal((int) p.getTotal());
        result.setPage(page);
        result.setPageSize(pageSize);
        if (records.isEmpty()) {
            result.setList(Collections.emptyList());
            return result;
        }

        Set<Integer> userIds = new HashSet<>();
        records.forEach(s -> {
            userIds.add(s.getUid());
            if (s.getReplyUid() != null) {
                userIds.add(s.getReplyUid());
            }
        });
        Map<Integer, User> userMap = loadUsers(userIds);
        result.setList(records.stream()
                .map(s -> toSubCommentVO(s, userMap))
                .collect(Collectors.toList()));
        return result;
    }

    @Override
    @Transactional
    public SubCommentVO addReply(long commentId, SubCommentDTO dto) {
        Long uid = currentUid();
        Comment comment = this.getById(commentId);
        if (comment == null || !Integer.valueOf(1).equals(comment.getStatus())) {
            throw new BusinessException("评论不存在");
        }

        Integer replyUid = comment.getUid();
        if (dto.getReplySubId() != null) {
            SubComment target = subCommentMapper.selectById(dto.getReplySubId());
            if (target == null
                    || !Integer.valueOf(1).equals(target.getStatus())
                    || !target.getCommentId().equals(commentId)) {
                throw new BusinessException("回复的评论不存在");
            }
            replyUid = target.getUid();
        }

        String content = normalizeContent(dto.getContent());
        SubComment sub = new SubComment()
                .setCommentId(commentId)
                .setReplySubId(dto.getReplySubId())
                .setUid(uid.intValue())
                .setReplyUid(replyUid)
                .setContent(content)
                .setStatus(1)
                .setCreatedAt(LocalDateTime.now());
        subCommentMapper.insert(sub);

        this.lambdaUpdate()
                .eq(Comment::getId, commentId)
                .setSql("reply_count = reply_count + 1")
                .update();
        postService.lambdaUpdate()
                .eq(Post::getId, comment.getPostId())
                .setSql("comment_count = comment_count + 1")
                .update();
        evictFeedPostAfterCommit(comment.getPostId()); // 回复数/评论数变了，事务提交后删快照

        Post post = postService.getById(comment.getPostId()); // 查出说说，通知规则需要说说作者
        if (post != null) { // 说说仍存在时才生成通知
            notificationService.notifyNewReply(post, comment, sub); // 通知被回复者，并在需要时通知说说作者
        }

        Set<Integer> ids = new HashSet<>();
        ids.add(uid.intValue());
        if (replyUid != null) {
            ids.add(replyUid);
        }
        return toSubCommentVO(sub, loadUsers(ids));
    }

    @Override
    @Transactional
    public void deleteComment(long commentId) {
        Long uid = currentUid();
        Comment comment = this.getById(commentId);
        if (comment == null || !Integer.valueOf(1).equals(comment.getStatus())) {
            throw new BusinessException("评论不存在");
        }

        Post post = postService.getById(comment.getPostId());
        if (post == null) {
            throw new BusinessException("说说不存在");
        }
        if (!uid.equals(comment.getUid().longValue()) && !uid.equals(post.getUid().longValue())) {
            throw new BusinessException("无权删除该评论");
        }

        int delta = 1 + (comment.getReplyCount() == null ? 0 : comment.getReplyCount());
        this.lambdaUpdate()
                .eq(Comment::getId, commentId)
                .set(Comment::getStatus, 0)
                .update();

        // 一级评论删除时，其下子评论一起软删除
        subCommentMapper.update(null, new LambdaUpdateWrapper<SubComment>()
                .eq(SubComment::getCommentId, commentId)
                .eq(SubComment::getStatus, 1)
                .set(SubComment::getStatus, 0));

        postService.lambdaUpdate()
                .eq(Post::getId, comment.getPostId())
                .setSql("comment_count = GREATEST(comment_count - " + delta + ", 0)")
                .update();
        evictFeedPostAfterCommit(comment.getPostId()); // 评论数减少，事务提交后删快照
    }

    @Override
    @Transactional
    public void deleteSubComment(long id) {
        Long uid = currentUid();
        SubComment sub = subCommentMapper.selectById(id);
        if (sub == null || !Integer.valueOf(1).equals(sub.getStatus())) {
            throw new BusinessException("回复不存在");
        }

        Comment comment = this.getById(sub.getCommentId());
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        Post post = postService.getById(comment.getPostId());
        if (post == null) {
            throw new BusinessException("说说不存在");
        }
        if (!uid.equals(sub.getUid().longValue()) && !uid.equals(post.getUid().longValue())) {
            throw new BusinessException("无权删除该回复");
        }

        subCommentMapper.update(null, new LambdaUpdateWrapper<SubComment>()
                .eq(SubComment::getId, id)
                .set(SubComment::getStatus, 0));

        this.lambdaUpdate()
                .eq(Comment::getId, comment.getId())
                .setSql("reply_count = GREATEST(reply_count - 1, 0)")
                .update();
        postService.lambdaUpdate()
                .eq(Post::getId, comment.getPostId())
                .setSql("comment_count = GREATEST(comment_count - 1, 0)")
                .update();
        evictFeedPostAfterCommit(comment.getPostId()); // 子评论删除，评论数减少，事务提交后删快照
    }

    private Long currentUid() {
        Long uid = BaseContext.getCurrentId();
        if (uid == null) {
            throw new LoginException("请先登录");
        }
        return uid;
    }

    /**
     * 事务提交后再失效快照，避免并发 Feed 读到提交前的旧评论数并缓存下来。
     */
    private void evictFeedPostAfterCommit(int postId) { // 评论数变化后，删这条帖子的内容缓存
        if (TransactionSynchronizationManager.isSynchronizationActive()) { // 当前有事务
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { // 注册事务回调
                @Override
                public void afterCommit() { // 事务提交成功后
                    postService.evictFeedPost(postId); // 再删快照，避免并发读缓存旧数据
                }
            });
        } else { // 没有事务
            postService.evictFeedPost(postId); // 立即删
        }
    }

    private String normalizeContent(String content) {
        String value = content == null ? "" : content.trim();
        if (value.isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }
        if (value.length() > 500) {
            throw new BusinessException("评论内容不能超过500字");
        }
        return value;
    }

    private Map<Integer, User> loadUsers(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userService.listByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    private SubCommentVO toSubCommentVO(SubComment s, Map<Integer, User> userMap) {
        User author = userMap.get(s.getUid());
        User replyTo = s.getReplyUid() == null ? null : userMap.get(s.getReplyUid());
        return SubCommentVO.builder()
                .id(s.getId())
                .commentId(s.getCommentId())
                .uid(s.getUid())
                .nickname(displayName(author))
                .avatar(author == null ? "" : author.getAvatar())
                .replyToUid(s.getReplyUid())
                .replyToNickname(replyTo == null ? "" : displayName(replyTo))
                .content(s.getContent())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private String displayName(User user) {
        if (user == null) {
            return "匿名";
        }
        if (user.getNickname() != null && !user.getNickname().isEmpty()) {
            return user.getNickname();
        }
        return user.getUsername() == null ? "匿名" : user.getUsername();
    }

}
