package com.example.secondhand_project.conf; // 配置类所在的包

import lombok.Data; // Lombok 注解，自动生成 getter/setter
import org.springframework.boot.context.properties.ConfigurationProperties; // 把 application.yml 里的配置绑定到这个类
import org.springframework.stereotype.Component; // 交给 Spring 管理

/**
 * 说说随机 Feed 的参数。
 *
 * <p>权重、页数、候选集范围都放在配置里，后续调参不用改代码。</p>
 */
@Data // 自动生成 getter/setter，方便其他地方读取配置
@Component // 注册成 Spring Bean
@ConfigurationProperties(prefix = "feed") // 读取 application.yml 里以 feed 开头的配置
public class FeedProperties {

    /** 默认返回条数 */
    private int size = 10; // 预留的默认条数；当前接口由 Controller 的 defaultValue=10 提供

    /** 单次最大返回条数 */
    private int maxSize = 20; // 一次最多返回 20 条，防止前端传 size=99999 把服务器拖垮

    /** 每个随机页的条数 */
    private int pageSize = 10; // 每一页放 10 个帖子 ID，和前端一次展示 10 条对应

    /** 最多预生成多少页 */
    private int pageCountMax = 500; // 最多生成 500 页，防止 Redis 里存太多数据

    /** 候选集时间范围（天） */
    private int candidateDays = 30; // 只拿最近 30 天的帖子参与随机，太老的帖子权重会很低

    /** 候选集上限 */
    private int candidateMax = 5000; // 一次最多查 5000 条候选，避免全表加载

    /** 时间衰减半衰期（小时） */
    private double halfLifeHours = 36; // 大约 36 小时后，时间分会衰减到 37%

    /** 时间分权重 */
    private double recencyWeight = 4.0; // 时间分最高贡献 4 分，越大新帖越容易冒头

    /** 点赞分权重 */
    private double likeWeight = 1.5; // 点赞分系数，越大高赞帖越容易冒头

    /** 新帖判定时间（小时） */
    private double newPostHours = 2; // 发布 2 小时内算新帖

    /** 新帖额外加成 */
    private double newPostBoost = 2.0; // 新帖额外加 2 分，保证刚发的内容容易被看到

    /** 权重上限，防止爆款霸屏 */
    private double maxWeight = 20.0; // 单条帖子权重最高 20，避免一条爆款占满所有页

    /** 单条说说快照缓存 TTL（秒） */
    private int snapshotTtlSeconds = 300; // 单条内容缓存 5 分钟

    /** 快照 TTL 随机抖动上限（秒） */
    private int snapshotTtlRandom = 60; // 再随机加 0~60 秒，避免大量缓存同时过期
}
