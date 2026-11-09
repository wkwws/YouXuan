package com.example.secondhand_project.job; // 定时任务所在的包

import com.example.secondhand_project.service.IFeedPoolService; // 调用随机池重建
import lombok.RequiredArgsConstructor; // 自动生成构造器，注入 final 字段
import lombok.extern.slf4j.Slf4j; // 自动生成 log 对象
import org.springframework.scheduling.annotation.Scheduled; // 定时任务注解
import org.springframework.stereotype.Component; // 交给 Spring 管理

/**
 * 每分钟重建一次随机页。
 *
 * <p>点赞数落库后权重不会立刻变化，下一次重建自然生效；这样点赞高峰不会触发大量全量重建。</p>
 */
@Component // 注册成 Spring Bean
@RequiredArgsConstructor // 自动生成构造器
@Slf4j // 自动生成 log
public class FeedPoolRebuildJob {

    private final IFeedPoolService feedPoolService; // 真正负责重建随机池的服务

    @Scheduled(fixedDelay = 60000) // 上一次执行完 60 秒后，再执行下一次
    public void rebuild() { // 定时入口
        try { // 定时任务不能因为一次异常就停掉
            feedPoolService.rebuild(); // 重新算权重、重新抽页
        } catch (Exception e) { // 重建失败时记录日志
            log.error("定时重建说说随机池失败", e); // 保留旧页，下一次再试
        }
    }
}
