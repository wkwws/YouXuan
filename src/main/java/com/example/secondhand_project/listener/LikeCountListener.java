package com.example.secondhand_project.listener;

import com.example.secondhand_project.constants.MqConstants;
import com.example.secondhand_project.enums.LikeBizTypeEnum;
import com.example.secondhand_project.pojo.dto.LikeCountMessage;
import com.example.secondhand_project.pojo.dto.LikedTimesDTO;
import com.example.secondhand_project.pojo.entity.Post;
import com.example.secondhand_project.service.IPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 点赞计数消费端：把 MQ 里的“绝对点赞数”写回 post.like_count。
 *
 * <p>
 * 模板 tj-remark 只负责“点赞关系 + 计数攒批 + 发 MQ”，
 * 真正的业务表更新由内容服务消费。本类就是融合进单体项目后的“内容服务”角色。
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LikeCountListener {

    private final IPostService postService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.Queue.LIKE_RECORD_QUEUE_POST, durable = "true"),
            exchange = @Exchange(
                    name = MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
                    type = ExchangeTypes.TOPIC
            ),
            key = MqConstants.Key.LIKED_TIMES_KEY_POST
    ))
    public void onLikeCount(LikeCountMessage message) {
        if (message == null || message.getList() == null || message.getList().isEmpty()) {
            return;
        }

        if (LikeBizTypeEnum.fromName(message.getBizType()) != LikeBizTypeEnum.POST) {
            log.warn("暂不支持的点赞业务类型: {}", message.getBizType());
            return;
        }

        boolean changed = false;
        for (LikedTimesDTO dto : message.getList()) {
            if (dto.getBizId() == null || dto.getLikedTimes() == null) {
                continue;
            }
            // 注意：写的是绝对计数，不是 +1，重复消费也安全
            boolean updated = postService.lambdaUpdate()
                    .eq(Post::getId, dto.getBizId().intValue())
                    .set(Post::getLikeCount, dto.getLikedTimes())
                    .update();
            changed = changed || updated;
        }

        if (changed) {
            // 大厅缓存里带的是旧计数，计数落库后统一失效，下次请求重建
            postService.evictFeedCache();
        }

        log.info("点赞计数落库完成, bizType={}, size={}", message.getBizType(), message.getList().size());
    }

}
