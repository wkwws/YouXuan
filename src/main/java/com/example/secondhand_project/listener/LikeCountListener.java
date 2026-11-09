package com.example.secondhand_project.listener; // 监听器所在的包

import com.example.secondhand_project.constants.MqConstants; // MQ 的交换机/队列/路由 key
import com.example.secondhand_project.enums.LikeBizTypeEnum; // 点赞业务类型
import com.example.secondhand_project.pojo.dto.LikeCountMessage; // MQ 消息体
import com.example.secondhand_project.pojo.dto.LikedTimesDTO; // 单条帖子的点赞数
import com.example.secondhand_project.pojo.entity.Post; // 说说实体
import com.example.secondhand_project.service.IPostService; // 说说服务
import lombok.RequiredArgsConstructor; // 自动生成构造器
import lombok.extern.slf4j.Slf4j; // 自动生成 log
import org.springframework.amqp.core.ExchangeTypes; // 交换机类型
import org.springframework.amqp.rabbit.annotation.Exchange; // 声明交换机
import org.springframework.amqp.rabbit.annotation.Queue; // 声明队列
import org.springframework.amqp.rabbit.annotation.QueueBinding; // 声明绑定关系
import org.springframework.amqp.rabbit.annotation.RabbitListener; // 监听队列
import org.springframework.stereotype.Component; // 交给 Spring 管理

/**
 * 点赞计数消费端：把 MQ 里的“绝对点赞数”写回 post.like_count。
 *
 * <p>
 * 模板 tj-remark 只负责“点赞关系 + 计数攒批 + 发 MQ”，
 * 真正的业务表更新由内容服务消费。本类就是融合进单体项目后的“内容服务”角色。
 * </p>
 */
@Component // 注册成 Spring Bean
@RequiredArgsConstructor // 自动生成构造器
@Slf4j // 自动生成 log
public class LikeCountListener {

    private final IPostService postService; // 用来更新点赞数和删快照

    @RabbitListener(bindings = @QueueBinding( // 监听点赞计数队列
            value = @Queue(name = MqConstants.Queue.LIKE_RECORD_QUEUE_POST, durable = "true"), // 队列名，持久化
            exchange = @Exchange( // 交换机
                    name = MqConstants.Exchange.LIKE_RECORD_EXCHANGE, // 交换机名
                    type = ExchangeTypes.TOPIC // topic 类型
            ),
            key = MqConstants.Key.LIKED_TIMES_KEY_POST // 路由 key
    ))
    public void onLikeCount(LikeCountMessage message) { // 收到一批点赞计数
        if (message == null || message.getList() == null || message.getList().isEmpty()) { // 空消息
            return; // 直接返回
        }

        if (LikeBizTypeEnum.fromName(message.getBizType()) != LikeBizTypeEnum.POST) { // 只处理“说说”的点赞
            log.warn("暂不支持的点赞业务类型: {}", message.getBizType()); // 其他类型先忽略
            return; // 直接返回
        }

        for (LikedTimesDTO dto : message.getList()) { // 一条一条处理
            if (dto.getBizId() == null || dto.getLikedTimes() == null) { // 数据不完整
                continue; // 跳过
            }
            // 注意：写的是绝对计数，不是 +1，重复消费也安全
            boolean updated = postService.lambdaUpdate() // 更新 post 表
                    .eq(Post::getId, dto.getBizId().intValue()) // 找到这条帖子
                    .set(Post::getLikeCount, dto.getLikedTimes()) // 直接写成 MQ 里的最新点赞数
                    .update(); // 执行更新
            if (updated) { // 如果确实更新了
                // 只失效这条说说的快照，随机池权重在下一次定时重建时更新
                postService.evictFeedPost(dto.getBizId().intValue()); // 删掉这条帖子的内容缓存
            }
        }

        log.info("点赞计数落库完成, bizType={}, size={}", message.getBizType(), message.getList().size()); // 记录日志
    }

}
