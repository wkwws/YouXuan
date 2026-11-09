package com.example.secondhand_project.service.imp; // 服务实现所在的包

import com.example.secondhand_project.conf.FeedProperties; // 读取权重/页数等参数
import com.example.secondhand_project.constants.RedisConstants; // Redis key 常量
import com.example.secondhand_project.mapper.PostMapper; // 查候选帖子
import com.example.secondhand_project.pojo.dto.FeedCandidate; // 候选帖子 DTO
import com.example.secondhand_project.service.IFeedPoolService; // 随机池接口
import com.example.secondhand_project.util.FeedWeightCalculator; // 算权重
import com.example.secondhand_project.util.RedislockUtil; // 分布式锁
import com.fasterxml.jackson.core.type.TypeReference; // 反序列化 List<Integer>
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 序列化/反序列化
import lombok.RequiredArgsConstructor; // 自动生成构造器
import lombok.extern.slf4j.Slf4j; // 自动生成 log
import org.springframework.data.redis.core.StringRedisTemplate; // 操作 Redis
import org.springframework.stereotype.Service; // 服务注解

import java.time.LocalDateTime; // 当前时间/时间下限
import java.util.ArrayList; // 可变列表
import java.util.Collections; // 返回空列表
import java.util.LinkedHashMap; // 保持插入顺序的 Map
import java.util.LinkedHashSet; // 保持插入顺序 + 去重
import java.util.List; // 列表
import java.util.Map; // Map
import java.util.concurrent.ThreadLocalRandom; // 线程安全的随机数
import java.util.concurrent.TimeUnit; // 时间单位

/**
 * 随机池实现：
 *
 * <ol>
 *   <li>查候选集（id / like_count / created_at）</li>
 *   <li>按时间 + 点赞算权重，得到累积权重数组</li>
 *   <li>每一页独立按权重不放回抽 10 条</li>
 *   <li>写入临时 Hash，再 RENAME 原子替换</li>
 * </ol>
 *
 * <p>为什么每一页要独立抽：如果只生成一份加权随机顺序再切页，每条帖子只会出现在固定一页里，
 * 随机选页就退化成均匀曝光，权重失效。</p>
 */
@Service // 交给 Spring 管理
@RequiredArgsConstructor // 自动生成构造器
@Slf4j // 自动生成 log
public class FeedPoolServiceImpl implements IFeedPoolService { // 实现随机池接口

    private static final int PAGE_TTL_SECONDS = 600; // 随机页在 Redis 里保留 10 分钟

    private static final int META_TTL_SECONDS = 600; // 元信息保留 10 分钟

    private final PostMapper postMapper; // 查候选帖子

    private final StringRedisTemplate stringRedisTemplate; // 读写 Redis

    private final ObjectMapper objectMapper; // 把 List<Integer> 转成 JSON

    private final FeedProperties feedProperties; // 权重/页数配置

    @Override
    public void rebuild() { // 重建整本随机名单
        if (!RedislockUtil.getlock(RedisConstants.FEED_POOL_LOCK, 60L, TimeUnit.SECONDS)) { // 抢重建锁，60 秒过期
            return; // 没抢到说明别人正在重建，直接返回
        }
        try { // 抢到锁后开始重建
            // 发帖/删帖不再触发重建；这里只由 60 秒定时任务或冷缓存调用。
            doRebuild(); // 真正重建：查候选、算权重、抽页、写 Redis
        } catch (Exception e) { // 重建失败
            log.error("重建说说随机池失败，保留旧随机页", e); // 记录日志，旧页继续服务
        } finally { // 无论成功失败
            RedislockUtil.unlock(RedisConstants.FEED_POOL_LOCK); // 一定要释放锁
        }
    }

    /**
     * 真正的重建逻辑。
     */
    private void doRebuild() throws Exception { // 可能抛 JSON/Redis 异常，交给外层处理
        LocalDateTime now = LocalDateTime.now(); // 当前时间
        LocalDateTime since = now.minusDays(Math.max(1, feedProperties.getCandidateDays())); // 候选时间下限，至少 1 天
        int candidateMax = Math.max(feedProperties.getPageSize(), feedProperties.getCandidateMax()); // 最多查多少条候选

        List<FeedCandidate> candidates = postMapper.selectFeedCandidates(since, candidateMax); // 查最近 N 天的候选
        // 小站如果最近 30 天不足一页，退化为全量最新数据，避免 Feed 为空
        if (candidates.size() < Math.max(10, feedProperties.getPageSize() * 3)) { // 候选太少
            candidates = postMapper.selectRecentCandidates(candidateMax); // 不限时间，直接拿最新候选
        }

        if (candidates == null || candidates.isEmpty()) { // 一条帖子都没有
            stringRedisTemplate.delete(RedisConstants.FEED_PAGES); // 删掉旧名单
            stringRedisTemplate.delete(RedisConstants.FEED_POOL_META); // 删掉旧元信息
            return; // 结束
        }

        double[] cumulative = new double[candidates.size()]; // 累积权重数组
        double total = 0; // 总权重
        for (int i = 0; i < candidates.size(); i++) { // 遍历每条候选
            FeedCandidate candidate = candidates.get(i); // 当前候选
            total += FeedWeightCalculator.weight( // 把这条帖子的权重累加到总权重
                    candidate.getLikeCount(), // 这条帖子有多少赞
                    candidate.getCreatedAt(), // 这条帖子什么时候发的
                    now, // 当前时间
                    feedProperties // 权重参数
            );
            cumulative[i] = total; // 记录“到这条为止的累积权重”
        }

        int pageSize = Math.max(1, feedProperties.getPageSize()); // 每页多少条，至少 1
        int pageCount = Math.max(6, (int) Math.ceil(candidates.size() * 1.5 / pageSize)); // 至少 6 页，约 1.5 倍覆盖
        pageCount = Math.min(pageCount, Math.max(6, feedProperties.getPageCountMax())); // 再受最大页数限制

        Map<String, String> pages = new LinkedHashMap<>(); // 准备写入的页：p0 -> JSON，p1 -> JSON...
        for (int page = 0; page < pageCount; page++) { // 每一页都独立抽
            List<Integer> ids = samplePage(candidates, cumulative, total, pageSize); // 按权重抽 10 个 ID
            pages.put("p" + page, objectMapper.writeValueAsString(ids)); // 转成 JSON 放进 Map
        }

        // 先写临时 key，再原子替换，读请求不会看到写了一半的 Hash
        stringRedisTemplate.delete(RedisConstants.FEED_PAGES_TMP); // 清掉上次可能残留的临时 key
        stringRedisTemplate.opsForHash().putAll(RedisConstants.FEED_PAGES_TMP, pages); // 新名单先写临时 key
        stringRedisTemplate.expire(RedisConstants.FEED_PAGES_TMP, PAGE_TTL_SECONDS, TimeUnit.SECONDS); // 临时 key 10 分钟过期
        stringRedisTemplate.rename(RedisConstants.FEED_PAGES_TMP, RedisConstants.FEED_PAGES); // 原子替换旧名单

        Map<String, String> meta = new LinkedHashMap<>(); // 元信息
        meta.put("pageCount", String.valueOf(pageCount)); // 一共几页
        meta.put("poolSize", String.valueOf(candidates.size())); // 一共多少条候选
        meta.put("builtAt", String.valueOf(System.currentTimeMillis())); // 什么时间生成的
        stringRedisTemplate.opsForHash().putAll(RedisConstants.FEED_POOL_META, meta); // 写入元信息
        stringRedisTemplate.expire(RedisConstants.FEED_POOL_META, META_TTL_SECONDS, TimeUnit.SECONDS); // 元信息 10 分钟过期

        log.info("说说随机池重建完成, poolSize={}, pageCount={}", candidates.size(), pageCount); // 记录重建结果
    }

    /**
     * 一页内按权重不放回抽 target 条。
     *
     * <p>累积权重 + 二分查找，重复抽到同一 ID 就重抽；小池子重抽次数多时按顺序补齐。</p>
     */
    private List<Integer> samplePage(List<FeedCandidate> candidates, // 全部候选
                                     double[] cumulative, // 累积权重
                                     double total, // 总权重
                                     int pageSize) { // 这一页要抽几条
        int target = Math.min(pageSize, candidates.size()); // 最多抽 min(每页条数, 候选数)
        LinkedHashSet<Integer> ids = new LinkedHashSet<>(target); // 用 Set 自动去重，保持抽到顺序
        if (target <= 0) { // 没有候选
            return new ArrayList<>(); // 返回空列表
        }
        if (total <= 0) { // 总权重异常（理论上不会发生，因为基础权重是 1）
            // 理论上不会发生（基础权重为 1），兜底为均匀抽样
            while (ids.size() < target) { // 一直抽到够数
                ids.add(candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())).getId()); // 随机拿一条
            }
            return new ArrayList<>(ids); // 返回结果
        }

        ThreadLocalRandom random = ThreadLocalRandom.current(); // 当前线程的随机数生成器
        int guard = 0; // 重抽次数
        int maxGuard = target * 20 + 50; // 防止极端权重下无限重抽
        while (ids.size() < target && guard++ < maxGuard) { // 还没抽满且没超过重抽上限
            double r = random.nextDouble() * total; // 在 [0, 总权重) 里随机扔一个点
            int index = upperBound(cumulative, r); // 找到这个点落在哪个区间
            ids.add(candidates.get(index).getId()); // 把这个 ID 放进这一页；Set 会自动去重
        }

        // 池子很小或权重极端时，重抽可能补不满，按顺序补齐
        if (ids.size() < target) { // 还没满
            for (FeedCandidate candidate : candidates) { // 按顺序遍历候选
                ids.add(candidate.getId()); // 补进去
                if (ids.size() >= target) { // 补满了
                    break; // 停止
                }
            }
        }
        return new ArrayList<>(ids); // 返回这一页的 ID 列表
    }

    /**
     * 返回第一个 cumulative[index] &gt; value 的下标。
     */
    private int upperBound(double[] cumulative, double value) { // 二分查找
        int low = 0; // 左边界
        int high = cumulative.length - 1; // 右边界
        while (low < high) { // 还没缩小到一个点
            int mid = (low + high) >>> 1; // 中间位置
            if (cumulative[mid] <= value) { // 目标点在右半边
                low = mid + 1; // 收缩左边界
            } else { // 目标点在左半边
                high = mid; // 收缩右边界
            }
        }
        return low; // 返回找到的下标
    }

    @Override
    public int pageCount() { // 当前有几页
        Long size = stringRedisTemplate.opsForHash().size(RedisConstants.FEED_PAGES); // HLEN feed:pages
        return size == null ? 0 : size.intValue(); // 没有 key 就返回 0
    }

    @Override
    public List<Integer> page(int index) { // 取第 index 页
        if (index < 0) { // 页码非法
            return Collections.emptyList(); // 返回空
        }
        Object value = stringRedisTemplate.opsForHash().get(RedisConstants.FEED_PAGES, "p" + index); // HGET feed:pages p{index}
        if (value == null) { // 这一页不存在
            return Collections.emptyList(); // 返回空
        }
        try { // 把 JSON 数组转成 List<Integer>
            return objectMapper.readValue(value.toString(), new TypeReference<List<Integer>>() { // 反序列化
            });
        } catch (Exception e) { // JSON 坏了
            log.warn("解析随机页失败, page={}", index, e); // 记录日志
            return Collections.emptyList(); // 按空页处理
        }
    }

    @Override
    public void evictPost(int postId) { // 只删一条帖子的内容缓存
        try { // Redis 异常不能影响主业务
            stringRedisTemplate.delete(RedisConstants.FEED_POST_CACHE + postId); // DEL feed:post:{postId}
        } catch (Exception e) { // Redis 操作失败
            log.warn("失效说说快照失败, postId={}", postId, e); // 记录日志即可
        }
    }
}
