package com.example.secondhand_project.constants;

public class RedisConstants {
    //在本项目项目中redis优化中会用到的常量管理

    //说说常量
    public static final String Post_Key="Second:post";

    public static final String Post_lock="Lock:post";

    // ==================== 说说大厅 ====================

    /** 大厅最新 10 条说说的缓存（只缓存内容，liked 由每次请求按用户叠加） */
    public static final String FEED_POST_LATEST = "feed:post:latest";

    /** 重建大厅缓存的互斥锁 */
    public static final String FEED_POST_LOCK = "lock:feed:post";

    // ==================== 点赞模块（融合 点赞功能模板/tj-remark） ====================

    /** 某个业务对象下的点赞用户集合：like:record:biz:{bizId} */
    public static final String LIKE_RECORD_BIZ = "like:record:biz:";

    /** 待同步的点赞计数 ZSet：like:record:count:{bizType} */
    public static final String LIKE_RECORD_COUNT = "like:record:count:";

}
