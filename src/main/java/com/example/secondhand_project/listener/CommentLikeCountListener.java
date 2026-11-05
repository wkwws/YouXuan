package com.example.secondhand_project.listener;

import com.example.secondhand_project.constants.MqConstants;
import com.example.secondhand_project.enums.LikeBizTypeEnum;
import com.example.secondhand_project.pojo.dto.LikeCountMessage;
import com.example.secondhand_project.pojo.dto.LikedTimesDTO;
import com.example.secondhand_project.pojo.entity.Comment;
import com.example.secondhand_project.service.ICommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 评论点赞计数消费端：把评论的绝对点赞数写回 comment.like_count。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommentLikeCountListener {

    private final ICommentService commentService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.Queue.LIKE_RECORD_QUEUE_COMMENT, durable = "true"),
            exchange = @Exchange(
                    name = MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
                    type = ExchangeTypes.TOPIC
            ),
            key = MqConstants.Key.LIKED_TIMES_KEY_COMMENT
    ))
    public void onCommentLikeCount(LikeCountMessage message) {
        if (message == null || message.getList() == null || message.getList().isEmpty()) {
            return;
        }
        if (LikeBizTypeEnum.fromName(message.getBizType()) != LikeBizTypeEnum.COMMENT) {
            log.warn("评论点赞消息业务类型不匹配: {}", message.getBizType());
            return;
        }

        for (LikedTimesDTO dto : message.getList()) {
            if (dto.getBizId() == null || dto.getLikedTimes() == null) {
                continue;
            }
            commentService.lambdaUpdate()
                    .eq(Comment::getId, dto.getBizId())
                    .set(Comment::getLikeCount, dto.getLikedTimes())
                    .update();
        }

        log.info("评论点赞计数落库完成, size={}", message.getList().size());
    }

}
