package com.example.secondhand_project.service.imp; // 服务实现所在的包

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 构造查询条件
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper; // 构造更新条件
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl; // MyBatis-Plus 基础服务
import com.example.secondhand_project.admin.service.AdminAuditTaskService; // 待审核队列
import com.example.secondhand_project.context.BaseContext; // 当前登录用户 ID
import com.example.secondhand_project.mapper.CommentMapper; // 评论表
import com.example.secondhand_project.mapper.PostMapper; // 说说表
import com.example.secondhand_project.mapper.SubCommentMapper; // 子评论表
import com.example.secondhand_project.pojo.dto.PostDTO; // 发布说说请求
import com.example.secondhand_project.pojo.entity.Comment; // 评论实体
import com.example.secondhand_project.pojo.entity.Post; // 说说实体
import com.example.secondhand_project.pojo.entity.SubComment; // 子评论实体
import com.example.secondhand_project.pojo.entity.User; // 用户实体
import com.example.secondhand_project.pojo.vo.PostVO; // 返回给前端的说说
import com.example.secondhand_project.service.IFeedPoolService; // 随机池服务
import com.example.secondhand_project.service.IPostService; // 说说服务接口
import com.example.secondhand_project.service.IUserService; // 用户服务
import com.fasterxml.jackson.databind.ObjectMapper; // 审核快照序列化
import lombok.RequiredArgsConstructor; // 自动生成构造器
import lombok.extern.slf4j.Slf4j; // 自动生成 log
import org.springframework.beans.BeanUtils; // 拷贝对象属性
import org.springframework.stereotype.Service; // 服务注解
import org.springframework.transaction.annotation.Transactional; // 事务注解
import org.springframework.transaction.support.TransactionSynchronization; // 事务回调
import org.springframework.transaction.support.TransactionSynchronizationManager; // 判断是否有事务

import java.time.LocalDateTime; // 当前时间
import java.util.LinkedHashMap; // 审核快照
import java.util.List; // 多条评论
import java.util.Map; // 审核快照
import java.util.stream.Collectors; // 流转 List

/**
 * <p>
 * 说说表（校园生活圈） 服务实现类
 * </p>
 *
 * @author wkw
 * @since 2026-05-31
 */
@Service // 交给 Spring 管理
@RequiredArgsConstructor // 自动生成构造器，注入 final 字段
@Slf4j // 自动生成 log
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService { // 继承基础 CRUD，实现自定义方法

    private final IUserService iUserService; // 查作者信息

    private final IFeedPoolService feedPoolService; // 失效随机池/单条快照

    private final CommentMapper commentMapper; // 删帖时处理评论

    private final SubCommentMapper subCommentMapper; // 删帖时处理子评论

    private final AdminAuditTaskService auditTaskService; // 发布后进入待审核队列

    private final ObjectMapper objectMapper; // 审核快照序列化

    @Override
    @Transactional // 说说和待审核任务必须一起成功或一起回滚
    public PostVO addpost(PostDTO post) { // 发布说说
        Post post1 = new Post(); // 创建数据库实体
        BeanUtils.copyProperties(post, post1); // 把前端传来的 content/images 拷进去

        Long currentId = BaseContext.getCurrentId(); // 当前登录用户 ID
        post1.setUid(((Number) currentId).intValue()); // 设置作者
        post1.setLikeCount(0); // 新帖点赞数从 0 开始
        post1.setCommentCount(0); // 新帖评论数从 0 开始
        post1.setAuditStatus(0); // 进入待审核队列（先发后审，默认仍可见）
        post1.setVisible(1);

        this.save(post1); // 插入 post 表
        auditTaskService.submit("POST", post1.getId(), post1.getUid(),
                snippet(post1.getContent()), post1.getContent(), snapshot(post1));
        // 发帖不重建随机池：等下一次 60 秒定时重建自然纳入，
        // 避免高并发发帖时反复删除随机页、造成缓存抖动和数据库回源。


        User byId = iUserService.getById(((Number) currentId).intValue()); // 查作者信息
        return PostVO.builder() // 组装返回给前端的对象
                .id(post1.getId()) // 说说 ID
                .uid(post1.getUid()) // 作者 ID
                .username(byId.getUsername()) // 用户名
                .nickname(byId.getNickname()) // 昵称
                .avatar(byId.getAvatar()) // 头像
                .content(post1.getContent()) // 内容
                .images(post1.getImages()) // 图片
                .likeCount(0) // 点赞数
                .commentCount(0) // 评论数
                .liked(false) // 当前用户还没点赞
                .createdAt(LocalDateTime.now()) // 发布时间
                .build(); // 构造完成
    }

    @Override
    @Transactional // 删帖涉及多张表，放在一个事务里
    public void deletepost(int id) { // 删除说说
        Post existing = this.getById(id); // 先查帖子是否存在
        if (existing == null) { // 不存在
            return; // 直接返回
        }

        // 删除说说时级联软删除它下面的评论和子评论，避免留下孤儿数据
        List<Long> commentIds = commentMapper.selectList( // 查这条帖子下的所有评论
                        new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, id) // 条件：post_id = 当前帖子
                ).stream() // 转成流
                .map(Comment::getId) // 只取评论 ID
                .collect(Collectors.toList()); // 收集成 List
        if (!commentIds.isEmpty()) { // 如果有评论
            subCommentMapper.update(null, new LambdaUpdateWrapper<SubComment>() // 更新子评论
                    .in(SubComment::getCommentId, commentIds) // 属于这些评论的子评论
                    .eq(SubComment::getStatus, 1) // 只处理正常的
                    .set(SubComment::getStatus, 0)); // 软删除：状态改成 0
        }
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>() // 更新一级评论
                .eq(Comment::getPostId, id) // 属于这条帖子
                .eq(Comment::getStatus, 1) // 只处理正常的
                .set(Comment::getStatus, 0)); // 软删除：状态改成 0

        this.removeById(id); // 物理删除 post 表里的这条说说
        auditTaskService.remove("POST", id);
        // 事务提交后只删单条快照；随机页里残留的旧 ID 不用管：
        // 读请求 MGET 不到快照、查库也查不到，会自动跳过；60 秒重建时自然清理。
        afterCommit(() -> feedPoolService.evictPost(id));
    }

    @Override
    public void evictFeedPost(int postId) { // 供点赞/评论服务调用
        feedPoolService.evictPost(postId); // 只删这一条帖子的内容缓存
    }

    private String snippet(String content) {
        if (content == null) {
            return "";
        }
        String text = content.trim();
        return text.length() <= 80 ? text : text.substring(0, 80) + "…";
    }

    private String snapshot(Post post) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", post.getId());
            map.put("content", post.getContent());
            map.put("images", post.getImages());
            map.put("uid", post.getUid());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 事务提交后再执行；没有事务就立即执行。
     */
    private void afterCommit(Runnable action) { // 包装一个“提交后执行”的工具
        if (TransactionSynchronizationManager.isSynchronizationActive()) { // 当前确实在事务里
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { // 注册事务回调
                @Override
                public void afterCommit() { // 事务提交成功后
                    action.run(); // 执行传进来的动作
                }
            });
        } else { // 没有事务
            action.run(); // 立即执行，避免什么都不做
        }
    }
}
