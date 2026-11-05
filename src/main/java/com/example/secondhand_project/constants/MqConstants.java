package com.example.secondhand_project.constants;

/**
 * MQ 常量（对应模板 tj-remark 里的 MqConstants）。
 *
 * <p>
 * 交换机 / 队列 / 绑定关系不再单独建 RabbitMQConfig 声明，
 * 直接在 LikeCountListener 的 @RabbitListener + @QueueBinding 上声明即可，
 * 和项目里现有的 RabbitLister 写法保持一致。
 * </p>
 */
public class MqConstants {

    public static class Exchange {
        /** 点赞计数交换机 */
        public static final String LIKE_RECORD_EXCHANGE = "like.record.exchange";
    }

    public static class Queue {
        /** 说说点赞计数队列 */
        public static final String LIKE_RECORD_QUEUE_POST = "like.record.queue.post";

        /** 评论点赞计数队列 */
        public static final String LIKE_RECORD_QUEUE_COMMENT = "like.record.queue.comment";
    }

    public static class Key {
        /** routing key 模板，%s 填业务类型 */
        public static final String LIKED_TIMES_KEY_TEMPLATE = "like.record.times.%s";

        /** 说说点赞 routing key（与 LikeBizTypeEnum.POST 的枚举名保持一致） */
        public static final String LIKED_TIMES_KEY_POST = "like.record.times.POST";

        /** 评论点赞 routing key（与 LikeBizTypeEnum.COMMENT 的枚举名保持一致） */
        public static final String LIKED_TIMES_KEY_COMMENT = "like.record.times.COMMENT";
    }

}
