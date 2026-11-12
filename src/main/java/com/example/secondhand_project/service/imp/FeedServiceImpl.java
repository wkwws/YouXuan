package com.example.secondhand_project.service.imp; // 服务实现所在的包

import com.example.secondhand_project.conf.FeedProperties; // 读取条数/TTL 等参数
import com.example.secondhand_project.constants.RedisConstants; // Redis key 常量
import com.example.secondhand_project.context.BaseContext; // 当前登录用户 ID
import com.example.secondhand_project.enums.LikeBizTypeEnum; // 点赞业务类型（说说）
import com.example.secondhand_project.pojo.entity.Post; // 说说实体
import com.example.secondhand_project.pojo.entity.User; // 用户实体
import com.example.secondhand_project.pojo.vo.PostVO; // 返回给前端的说说
import com.example.secondhand_project.service.IFeedPoolService; // 随机池服务
import com.example.secondhand_project.service.IFeedService; // 随机 Feed 接口
import com.example.secondhand_project.service.ILikedRecordService; // 点赞状态服务
import com.example.secondhand_project.service.IPostService; // 说说基础服务
import com.example.secondhand_project.service.IUserService; // 用户服务
import com.example.secondhand_project.util.FeedWeightCalculator; // 权重公式
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 序列化/反序列化
import lombok.RequiredArgsConstructor; // 自动生成构造器
import lombok.extern.slf4j.Slf4j; // 自动生成 log
import org.springframework.data.redis.core.StringRedisTemplate; // 操作 Redis
import org.springframework.stereotype.Service; // 服务注解

import java.time.LocalDateTime; // 当前时间
import java.util.ArrayList; // 可变列表
import java.util.Collections; // 空集合
import java.util.HashSet; // 普通 Set
import java.util.LinkedHashMap; // 保持顺序的 Map
import java.util.LinkedHashSet; // 保持顺序 + 去重
import java.util.List; // 列表
import java.util.Map; // Map
import java.util.Objects; // 过滤 null
import java.util.Set; // Set
import java.util.concurrent.ThreadLocalRandom; // 线程安全随机数
import java.util.concurrent.TimeUnit; // 时间单位
import java.util.stream.Collectors; // 流转 List

/**
 * 随机 Feed 读取：
 *
 * <ol>
 *   <li>从 Redis 随机页拿 ID</li>
 *   <li>MGET 单条快照，缺失的批量查库并回填</li>
 *   <li>按当前用户批量填充 liked</li>
 * </ol>
 *
 * <p>Redis 不可用或随机页为空时，降级为查库 + 内存加权抽样。</p>
 */
@Service // 交给 Spring 管理
@RequiredArgsConstructor // 自动生成构造器
@Slf4j // 自动生成 log
public class FeedServiceImpl implements IFeedService { // 实现随机 Feed 接口

    private final IPostService postService; // 批量查缺失帖子、降级查库

    private final IUserService userService; // 批量查作者信息

    private final ILikedRecordService likedRecordService; // 批量查当前用户是否点赞

    private final IFeedPoolService feedPoolService; // 读随机页、冷缓存触发重建、删快照

    private final StringRedisTemplate stringRedisTemplate; // MGET/SET 单条快照

    private final ObjectMapper objectMapper; // 快照 JSON 序列化/反序列化

    private final FeedProperties feedProperties; // 条数上限、快照 TTL 等参数

    @Override
    public List<PostVO> randomFeed(int size, List<Integer> exclude) { // 随机 Feed 主流程
        int limit = Math.min(Math.max(size, 1), Math.max(1, feedProperties.getMaxSize())); // 把条数夹在 1~20 之间
        Set<Integer> excluded = exclude == null // 如果前端没传 exclude
                ? Collections.emptySet() // 就用空集合
                : exclude.stream() // 否则把 List 转成流
                        .filter(Objects::nonNull) // 去掉 null
                        .limit(50) // 最多处理 50 个，防止参数太长
                        .collect(Collectors.toSet()); // 收集成 Set，方便 O(1) 判断

        try { // 正常走 Redis 随机页
            int pageCount = ensurePageCount(); // 确保 Redis 里有随机页
            if (pageCount <= 0) { // 还是没有页（Redis 不可用/重建失败）
                return fillLiked(fallbackFromDb(limit, excluded)); // 降级查库并填 liked
            }

            // 多拿几条，容忍随机页里包含已删除的帖子
            List<Integer> ids = collectIds(pageCount, limit + 10, excluded, false); // 从随机页收集 ID，过滤上一批
            if (ids.isEmpty()) { // 如果过滤后一个都不剩
                // 小池子且 exclude 覆盖了全部内容时，允许重复上一批
                ids = collectIds(pageCount, limit + 10, excluded, true); // 允许重复上一批再收集一次
            }
            if (ids.isEmpty()) { // 还是为空
                return fillLiked(fallbackFromDb(limit, excluded)); // 直接降级查库
            }

            List<PostVO> list = loadSnapshots(ids); // 根据 ID 读内容快照，缺失的查库并回填
            if (list.size() > limit) { // 多拿的 ID 可能超过需要条数
                list = new ArrayList<>(list.subList(0, limit)); // 截断到 limit 条
            }

            // 如果随机页里有已删除帖子导致不足，降级补几条
            if (list.size() < limit) { // 数量不够
                Set<Integer> have = list.stream() // 已经拿到的帖子
                        .map(PostVO::getId) // 只取 ID
                        .collect(Collectors.toSet()); // 收集成 Set
                Set<Integer> moreExclude = new HashSet<>(excluded); // 复制一份 exclude
                moreExclude.addAll(have); // 把已经拿到的也加进去，避免重复
                for (PostVO vo : fallbackFromDb(limit - list.size(), moreExclude)) { // 降级补差额
                    if (have.add(vo.getId())) { // 如果这条之前没拿到
                        list.add(vo); // 加进结果
                    }
                    if (list.size() >= limit) { // 补够了
                        break; // 停止
                    }
                }
            }
            return fillLiked(list); // 最后按当前用户填 liked，返回
        } catch (Exception e) { // Redis/解析等任何异常
            log.warn("读取说说随机池失败，降级查库", e); // 记录日志
            return fillLiked(fallbackFromDb(limit, excluded)); // 降级查库
        }
    }

    /**
     * 确保随机页存在。
     *
     * <p>缓存刚失效时可能有多个请求同时进来：拿到锁的线程重建，
     * 其他线程短暂等待，避免全部降级去查库。</p>
     */
    private int ensurePageCount() { // 返回当前页数；没有就触发重建
        int pageCount = feedPoolService.pageCount(); // 先看 Redis 里有没有页
        if (pageCount > 0) { // 有页
            return pageCount; // 直接返回
        }

        feedPoolService.rebuild(); // 没页就触发一次重建
        for (int i = 0; i < 10; i++) { // 最多等 10 次
            pageCount = feedPoolService.pageCount(); // 再看一次
            if (pageCount > 0) { // 有页了
                return pageCount; // 返回
            }
            try { // 等 50ms 再试
                Thread.sleep(50); // 让出一点时间给重建线程
            } catch (InterruptedException e) { // 被中断
                Thread.currentThread().interrupt(); // 恢复中断标记
                break; // 不再等
            }
        }
        return pageCount; // 返回最后的结果（可能还是 0）
    }

    /**
     * 从随机页收集 ID。allowExcluded=false 时过滤掉上一批；true 时允许重复上一批。
     */
    private List<Integer> collectIds(int pageCount, int target, Set<Integer> excluded, boolean allowExcluded) { // 收集 ID
        LinkedHashSet<Integer> chosen = new LinkedHashSet<>(); // 用 LinkedHashSet 去重并保持顺序
        Set<Integer> usedPages = new HashSet<>(); // 记录已经抽过哪些页，避免重复抽同一页
        int attempts = Math.min(pageCount, 8); // 最多抽 8 页
        ThreadLocalRandom random = ThreadLocalRandom.current(); // 随机数

        while (usedPages.size() < attempts && chosen.size() < target) { // 还没抽够页数/ID 数
            int index = random.nextInt(pageCount); // 随机选一页
            if (!usedPages.add(index)) { // 如果这页已经抽过
                continue; // 换一页
            }
            for (Integer id : feedPoolService.page(index)) { // 遍历这一页的 10 个 ID
                if (id == null) { // 脏数据
                    continue; // 跳过
                }
                if (!allowExcluded && excluded.contains(id)) { // 不允许重复且命中上一批
                    continue; // 跳过
                }
                chosen.add(id); // 放进结果
                if (chosen.size() >= target) { // 够数了
                    break; // 停止遍历
                }
            }
        }
        return new ArrayList<>(chosen); // 转成 List 返回
    }

    /**
     * 先读快照，缺失的批量查库并回填 Redis。
     */
    private List<PostVO> loadSnapshots(List<Integer> ids) { // 根据 ID 读内容
        if (ids == null || ids.isEmpty()) { // 没有 ID
            return new ArrayList<>(); // 返回空
        }

        List<String> keys = ids.stream() // 把每个 ID 变成 Redis key
                .map(id -> RedisConstants.FEED_POST_CACHE + id) // feed:post:{id}
                .collect(Collectors.toList()); // 收集成 List
        List<String> cached = stringRedisTemplate.opsForValue().multiGet(keys); // 一次性 MGET 所有快照

        Map<Integer, PostVO> found = new LinkedHashMap<>(); // 已经找到的内容
        List<Integer> missing = new ArrayList<>(); // 没命中的 ID
        for (int i = 0; i < ids.size(); i++) { // 遍历每个 ID
            Integer id = ids.get(i); // 当前 ID
            PostVO vo = parseSnapshot(cached == null ? null : cached.get(i)); // 尝试把缓存 JSON 转成对象
            if (vo == null) { // 没命中或解析失败
                missing.add(id); // 记到 missing
            } else { // 命中
                found.put(id, vo); // 放进 found
            }
        }

        if (!missing.isEmpty()) { // 有没命中的
            List<Post> posts = postService.listByIds(missing); // 批量查数据库
            if (posts != null) { // 查到了；过滤掉刚被隐藏/驳回的帖子
                posts = posts.stream()
                        .filter(post -> post.getVisible() == null || post.getVisible() == 1)
                        .filter(post -> post.getAuditStatus() == null || post.getAuditStatus() != 2)
                        .collect(Collectors.toList());
            }
            if (posts != null && !posts.isEmpty()) { // 查到了
                Map<Integer, User> users = loadUsers(posts.stream() // 收集作者 ID
                        .map(Post::getUid) // 只取 uid
                        .collect(Collectors.toSet())); // 去重
                for (Post post : posts) { // 遍历查到的帖子
                    PostVO vo = toVO(post, users.get(post.getUid())); // 组装成前端对象
                    found.put(post.getId(), vo); // 放进结果
                    cacheSnapshot(vo); // 回填 Redis 快照
                }
            }
        }

        List<PostVO> result = new ArrayList<>(found.size()); // 按原始 ID 顺序返回
        for (Integer id : ids) { // 遍历原始 ID
            PostVO vo = found.get(id); // 取对应内容
            if (vo != null) { // 如果存在
                result.add(vo); // 加进结果
            }
        }
        return result; // 返回
    }

    private PostVO parseSnapshot(String json) { // 把缓存 JSON 转成 PostVO
        if (json == null || json.isEmpty()) { // 没有缓存
            return null; // 返回 null 表示未命中
        }
        try { // 正常解析
            return objectMapper.readValue(json, PostVO.class); // JSON -> PostVO
        } catch (Exception e) { // 缓存格式坏了
            log.warn("解析说说快照失败，按未命中处理", e); // 记录日志
            return null; // 当没命中
        }
    }

    private void cacheSnapshot(PostVO vo) { // 写单条快照
        if (vo == null || vo.getId() == null) { // 数据不完整
            return; // 不写
        }
        try { // 正常写
            int ttl = Math.max(1, feedProperties.getSnapshotTtlSeconds()); // 基础 TTL，至少 1 秒
            int random = Math.max(0, feedProperties.getSnapshotTtlRandom()); // 随机抖动上限
            if (random > 0) { // 需要抖动
                ttl += ThreadLocalRandom.current().nextInt(random + 1); // 加 0~random 秒
            }
            stringRedisTemplate.opsForValue().set( // 写入 Redis
                    RedisConstants.FEED_POST_CACHE + vo.getId(), // key: feed:post:{id}
                    objectMapper.writeValueAsString(vo), // value: PostVO JSON
                    ttl, // 过期时间
                    TimeUnit.SECONDS // 单位：秒
            );
        } catch (Exception e) { // 写失败
            log.warn("写入说说快照失败, postId={}", vo.getId(), e); // 记录日志，不影响返回
        }
    }

    private List<PostVO> fillLiked(List<PostVO> list) { // 填当前用户的点赞状态
        if (list == null || list.isEmpty()) { // 空列表
            return list == null ? new ArrayList<>() : list; // 返回空列表
        }
        Long uid = BaseContext.getCurrentId(); // 当前登录用户
        if (uid == null) { // 未登录
            list.forEach(post -> post.setLiked(false)); // 全部未点赞
            return list; // 返回
        }
        try { // 登录用户批量查
            List<Long> ids = list.stream() // 当前这批帖子
                    .map(post -> post.getId().longValue()) // 转成 Long
                    .collect(Collectors.toList()); // 收集成 List
            Set<Long> liked = new HashSet<>(likedRecordService.showUserLike(LikeBizTypeEnum.POST, ids)); // 查哪些已点赞
            list.forEach(post -> post.setLiked(liked.contains(post.getId().longValue()))); // 逐条设置
        } catch (Exception e) { // 查询失败
            log.warn("批量查询点赞状态失败，liked 全部置为 false", e); // 记录日志
            list.forEach(post -> post.setLiked(false)); // 全部按未点赞
        }
        return list; // 返回
    }

    /**
     * Redis 不可用/随机池为空时的降级：查最近 200 条，在内存里按权重抽样。
     */
    private List<PostVO> fallbackFromDb(int size, Set<Integer> exclude) { // 降级查库
        try { // 正常查
            int fetch = Math.max(size * 20, 200); // 一次最多查多少条，至少 200
            List<Post> posts = postService.lambdaQuery() // 用 MyBatis-Plus 查
                    .eq(Post::getVisible, 1) // 隐藏的不进 Feed
                    .ne(Post::getAuditStatus, 2) // 驳回的不进 Feed
                    .orderByDesc(Post::getCreatedAt) // 按时间倒序
                    .last("LIMIT " + fetch) // 只取最近 fetch 条
                    .list(); // 执行
            if (posts == null || posts.isEmpty()) { // 没有帖子
                return new ArrayList<>(); // 返回空
            }

            List<Post> candidates = posts.stream() // 过滤 exclude
                    .filter(post -> !exclude.contains(post.getId())) // 去掉上一批
                    .collect(Collectors.toList()); // 收集成 List
            if (candidates.size() < size) { // 过滤后不够
                candidates = posts; // 小池子就允许重复上一批
            }
            if (candidates.isEmpty()) { // 还是空
                return new ArrayList<>(); // 返回空
            }

            List<Post> picked = weightedSample(candidates, size); // 在内存里按权重抽
            Map<Integer, User> users = loadUsers(picked.stream() // 收集作者 ID
                    .map(Post::getUid) // 只取 uid
                    .collect(Collectors.toSet())); // 去重
            return picked.stream() // 组装成 PostVO
                    .map(post -> toVO(post, users.get(post.getUid()))) // Post + User -> PostVO
                    .collect(Collectors.toList()); // 收集成 List
        } catch (Exception e) { // 数据库也失败
            log.error("Feed 降级查库失败", e); // 记录日志
            return new ArrayList<>(); // 返回空
        }
    }

    private List<Post> weightedSample(List<Post> posts, int size) { // 内存版加权抽样
        LocalDateTime now = LocalDateTime.now(); // 当前时间
        double[] cumulative = new double[posts.size()]; // 累积权重
        double total = 0; // 总权重
        for (int i = 0; i < posts.size(); i++) { // 遍历候选
            Post post = posts.get(i); // 当前帖子
            total += FeedWeightCalculator.weight( // 累加权重
                    post.getLikeCount(), // 点赞数
                    post.getCreatedAt(), // 发布时间
                    now, // 当前时间
                    feedProperties // 参数
            );
            cumulative[i] = total; // 记录累积值
        }

        int target = Math.min(size, posts.size()); // 最多抽多少条
        LinkedHashSet<Integer> indexes = new LinkedHashSet<>(target); // 抽中的下标，去重
        ThreadLocalRandom random = ThreadLocalRandom.current(); // 随机数
        int guard = 0; // 重抽次数
        int maxGuard = target * 20 + 50; // 防止无限重抽
        while (indexes.size() < target && guard++ < maxGuard) { // 还没抽满
            double r = random.nextDouble() * Math.max(total, 1e-9); // 随机点
            int low = 0; // 二分左边界
            int high = cumulative.length - 1; // 二分右边界
            while (low < high) { // 二分查找
                int mid = (low + high) >>> 1; // 中间位置
                if (cumulative[mid] <= r) { // 在右半边
                    low = mid + 1; // 收缩左边
                } else { // 在左半边
                    high = mid; // 收缩右边
                }
            }
            indexes.add(low); // 记下抽中的下标
        }
        if (indexes.size() < target) { // 没抽满
            for (int i = 0; i < posts.size() && indexes.size() < target; i++) { // 按顺序补齐
                indexes.add(i); // 补一个
            }
        }
        return indexes.stream().map(posts::get).collect(Collectors.toList()); // 下标转成帖子
    }

    private Map<Integer, User> loadUsers(Set<Integer> uids) { // 批量查作者
        if (uids == null || uids.isEmpty()) { // 没有作者 ID
            return Collections.emptyMap(); // 返回空 Map
        }
        return userService.listByIds(uids).stream() // 批量查用户
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a)); // id -> User
    }

    private PostVO toVO(Post post, User author) { // 把数据库实体转成前端对象
        return PostVO.builder() // 使用 Builder
                .id(post.getId()) // 帖子 ID
                .uid(post.getUid()) // 作者 ID
                .username(author == null || author.getUsername() == null ? "" : author.getUsername()) // 用户名
                .nickname(author == null || author.getNickname() == null ? "" : author.getNickname()) // 昵称
                .avatar(author == null || author.getAvatar() == null ? "" : author.getAvatar()) // 头像
                .content(post.getContent()) // 内容
                .images(post.getImages()) // 图片
                .likeCount(post.getLikeCount() == null ? 0 : post.getLikeCount()) // 点赞数
                .commentCount(post.getCommentCount() == null ? 0 : post.getCommentCount()) // 评论数
                .liked(false) // 点赞状态后面按用户填
                .createdAt(post.getCreatedAt()) // 发布时间
                .build(); // 构造完成
    }
}
