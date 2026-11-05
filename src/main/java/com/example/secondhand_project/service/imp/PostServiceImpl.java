package com.example.secondhand_project.service.imp;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secondhand_project.constants.RedisConstants;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.enums.LikeBizTypeEnum;
import com.example.secondhand_project.mapper.CommentMapper;
import com.example.secondhand_project.mapper.PostMapper;
import com.example.secondhand_project.mapper.SubCommentMapper;
import com.example.secondhand_project.pojo.dto.PostDTO;
import com.example.secondhand_project.pojo.entity.Comment;
import com.example.secondhand_project.pojo.entity.Post;
import com.example.secondhand_project.pojo.entity.SubComment;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.pojo.vo.PostVO;
import com.example.secondhand_project.service.ILikedRecordService;
import com.example.secondhand_project.service.IPostService;
import com.example.secondhand_project.service.IUserService;
import com.example.secondhand_project.util.RedislockUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 说说表（校园生活圈） 服务实现类
 * </p>
 *
 * @author wkw
 * @since 2026-05-31
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {

    /**
     * 大厅一次返回 10 条，后续换成推荐/随机召回时改这里即可
     */
    private static final int FEED_LIMIT = 10;

    private static final long FEED_TTL_SECONDS = 60;

    private static final long FEED_TTL_RANDOM = 20;

    private final IUserService iUserService;

    private final StringRedisTemplate stringRedisTemplate;

    private final ILikedRecordService likedRecordService;

    private final CommentMapper commentMapper;

    private final SubCommentMapper subCommentMapper;

    private final ObjectMapper objectMapper;

    @Override
    public PostVO addpost(PostDTO post) {
        Post post1 = new Post();
        BeanUtils.copyProperties(post, post1);

        Long currentId = BaseContext.getCurrentId();
        post1.setUid(((Number) currentId).intValue());
        post1.setLikeCount(0);
        post1.setCommentCount(0);

        this.save(post1);
        evictFeedCache();

        User byId = iUserService.getById(((Number) currentId).intValue());
        return PostVO.builder()
                .id(post1.getId())
                .uid(post1.getUid())
                .username(byId.getUsername())
                .nickname(byId.getNickname())
                .avatar(byId.getAvatar())
                .content(post1.getContent())
                .images(post1.getImages())
                .likeCount(0)
                .commentCount(0)
                .liked(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public List<PostVO> listLatest() {
        String json = stringRedisTemplate.opsForValue().get(RedisConstants.FEED_POST_LATEST);
        List<PostVO> list = readCache(json);
        if (list == null) {
            //没有查到的话，尝试获取锁然后找数据库
            list = loadLatestFromDbWithLock();
        }
        fillLiked(list);
        return list;
    }

    private List<PostVO> loadLatestFromDbWithLock() {
        /**
         * 这个函数主要是第一次没有从缓存中获取到说说的后操作， 获取 需要查的说说 消息，
         * 先通过获取分布式锁，获取不到就进入循环，20次循环 ，每次循环都会尝试获取缓存中的数据
         * 如果20次循环后，还没有从redis 中获取到缓存，就会 从数据库总获取
         * 如果获取到锁，就从数据库总获取说说的信息，然后 存储到redis 中，最后释放锁
         * 整体流程还是可以看懂的
         */

        if (!RedislockUtil.getlock(RedisConstants.FEED_POST_LOCK)) {
            // 其他线程正在重建缓存，短暂等待；仍拿不到就降级直接查库
            for (int i = 0; i < 20; i++) {
                String cached = stringRedisTemplate.opsForValue().get(RedisConstants.FEED_POST_LATEST);
                List<PostVO> cachedList = readCache(cached);
                if (cachedList != null) {
                    return cachedList;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return loadLatestFromDb();
        }

        try {
            // 双重检查：拿到锁之后可能别的线程已经写好了缓存
            String cached = stringRedisTemplate.opsForValue().get(RedisConstants.FEED_POST_LATEST);
            List<PostVO> cachedList = readCache(cached);
            if (cachedList != null) {
                return cachedList;
            }

            List<PostVO> list = loadLatestFromDb();
            long ttl = FEED_TTL_SECONDS + RandomUtil.randomLong(FEED_TTL_RANDOM);
            writeCache(list, ttl);
            return list;
        } finally {
            RedislockUtil.unlock(RedisConstants.FEED_POST_LOCK);
        }
    }

    private List<PostVO> loadLatestFromDb() {
        List<Post> posts = this.lambdaQuery()
                .orderByDesc(Post::getCreatedAt)
                .last("LIMIT " + FEED_LIMIT)
                .list();
        if (posts.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> uids = posts.stream().map(Post::getUid).collect(Collectors.toSet());
        Map<Integer, User> userMap = iUserService.listByIds(uids).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return posts.stream().map(p -> {
            User author = userMap.get(p.getUid());
            return PostVO.builder()
                    .id(p.getId())
                    .uid(p.getUid())
                    .username(author == null ? "" : author.getUsername())
                    .nickname(author == null ? "" : author.getNickname())
                    .avatar(author == null ? "" : author.getAvatar())
                    .content(p.getContent())
                    .images(p.getImages())
                    .likeCount(p.getLikeCount() == null ? 0 : p.getLikeCount())
                    .commentCount(p.getCommentCount() == null ? 0 : p.getCommentCount())
                    .liked(false)
                    .createdAt(p.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private void fillLiked(List<PostVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Long uid = BaseContext.getCurrentId();
        if (uid == null) {
            list.forEach(p -> p.setLiked(false));
            return;
        }

        List<Long> ids = list.stream()
                .map(p -> p.getId().longValue())
                .collect(Collectors.toList());
        Set<Long> likedSet = new HashSet<>(
                likedRecordService.showUserLike(LikeBizTypeEnum.POST, ids)
        );
        list.forEach(p -> p.setLiked(likedSet.contains(p.getId().longValue())));
    }

    @Override
    @Transactional
    public void deletepost(int id) {
        Post existing = this.getById(id);
        if (existing == null) {
            return;
        }

        // 删除说说时级联软删除它下面的评论和子评论，避免留下孤儿数据
        List<Long> commentIds = commentMapper.selectList(
                        new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, id)
                ).stream()
                .map(Comment::getId)
                .collect(Collectors.toList());
        if (!commentIds.isEmpty()) {
            subCommentMapper.update(null, new LambdaUpdateWrapper<SubComment>()
                    .in(SubComment::getCommentId, commentIds)
                    .eq(SubComment::getStatus, 1)
                    .set(SubComment::getStatus, 0));
        }
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .eq(Comment::getPostId, id)
                .eq(Comment::getStatus, 1) //这个是设计的逻辑删除，用来备份
                .set(Comment::getStatus, 0));

        this.removeById(id);
        evictFeedCache();
    }

    @Override
    public void evictFeedCache() {
        stringRedisTemplate.delete(RedisConstants.FEED_POST_LATEST);
    }

    private List<PostVO> readCache(String json) {
        if (json == null) {
            return null;
        }
        try {
//            return objectMapper.readValue(json, new TypeReference<List<PostVO>>() {
//            });

            //这个我更习惯用 hutool 工具包中的 工具，因为写起来更简单
            List<PostVO> list = JSONUtil.toList(json, PostVO.class);
            return list;
        } catch (Exception e) {
            log.warn("解析说说大厅缓存失败，降级查库", e);
            return null;
        }
    }

    private void writeCache(List<PostVO> list, long ttl) {
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisConstants.FEED_POST_LATEST,
                    objectMapper.writeValueAsString(list),
                    ttl,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.warn("写入说说大厅缓存失败", e);
        }
    }

}
