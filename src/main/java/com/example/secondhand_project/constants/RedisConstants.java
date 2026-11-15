package com.example.secondhand_project.constants; // 常量类所在的包

public class RedisConstants { // 集中管理 Redis key，避免代码里到处写字符串
    //在本项目项目中redis优化中会用到的常量管理

    //说说常量
    public static final String Post_Key="Second:post"; // 旧常量，保留兼容

    public static final String Post_lock="Lock:post"; // 旧常量，保留兼容

    // ==================== 说说大厅（加权随机） ====================

    /** 预生成的加权随机页：Hash，p0/p1/... -> JSON 数组 */
    public static final String FEED_PAGES = "feed:pages"; // 一本“名单册”，里面有很多页，每页 10 个帖子 ID

    /** 重建时的临时 Hash，写完后 RENAME 到 FEED_PAGES */
    public static final String FEED_PAGES_TMP = "feed:pages:tmp"; // 新名单先写这里，写完再整体替换旧名单

    /** 随机池元信息：pageCount / poolSize / builtAt */
    public static final String FEED_POOL_META = "feed:pool:meta"; // 记录页数、候选数、生成时间，方便排查问题

    /** 单条说说快照：feed:post:{id} */
    public static final String FEED_POST_CACHE = "feed:post:"; // 单条说说内容缓存的前缀

    /** 重建随机页的互斥锁 */
    public static final String FEED_POOL_LOCK = "lock:feed:pool"; // 同一时间只允许一个线程重建

    // 说明：发帖不重建随机池，等 60 秒定时重建自然纳入；
    // 删帖只删 feed:post:{id} 单条快照，随机页里的旧 ID 由读请求跳过。
    // 因此不再需要 feed:pool:dirty 脏标记。

    // ==================== 点赞模块（融合 点赞功能模板/tj-remark） ====================

    /** 某个业务对象下的点赞用户集合：like:record:biz:{bizId} */
    public static final String LIKE_RECORD_BIZ = "like:record:biz:"; // 谁点赞了这条帖子/评论

    /** 待同步的点赞计数 ZSet：like:record:count:{bizType} */
    public static final String LIKE_RECORD_COUNT = "like:record:count:"; // 还没落库的点赞数

    // ==================== 接口限流 ====================

    /** 限流 ZSET 的前缀：rate_limit:{接口标识}:{身份标识} */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

}
