package com.example.secondhand_project.service; // 服务接口所在的包

import java.util.List; // 返回一页 ID 需要 List

/**
 * 说说随机池：负责按权重预生成随机页，以及失效单条快照。
 */
public interface IFeedPoolService {

    /**
     * 重建随机页。内部带分布式锁，重复调用是安全的。
     */
    void rebuild(); // 重新算权重、重新抽所有页，并写回 Redis

    /**
     * 当前可用的随机页数量。
     */
    int pageCount(); // 告诉调用方 Redis 里有几页可以抽

    /**
     * 取第 index 页的帖子 ID。
     */
    List<Integer> page(int index); // 取出一页的 10 个帖子 ID

    /**
     * 失效单条说说的快照（点赞数/评论数/内容变化时调用）。
     *
     * <p>发帖不重建随机池，等 60 秒定时重建自然纳入；
     * 删帖只删单条快照，随机页里的旧 ID 读请求会自动跳过。</p>
     */
    void evictPost(int postId); // 只删这一条帖子的内容缓存，不动整本名单
}
