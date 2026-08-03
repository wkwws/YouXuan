package com.example.secondhand_project.listener;

import cn.hutool.json.JSONUtil;
import com.example.secondhand_project.event.MessageSavedEvent;
import com.example.secondhand_project.pojo.entity.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 消息保存成功 → 事务提交后投递 RabbitMQ 的监听器
 *
 * 解决隐患：@Transactional 只对数据库操作生效，管不到 RabbitMQ。
 * 如果在事务内直接 convertAndSend，消息会在 DB 提交前就进入队列，
 * 一旦后续事务回滚，就会出现"推送了但库里没记录"的脏数据。
 *
 * 本类使用 @TransactionalEventListener(phase = AFTER_COMMIT)：
 * 只有 addmessage 所在事务【提交成功】后，本方法才会被触发，
 * 从机制上保证"数据库里有这条消息，才投递到 MQ 推送"。
 */
@Component
public class MessagePushListener {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 事务提交成功后才执行
     *
     * @param event 事务内发布的消息保存事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSaved(MessageSavedEvent event) {
        // 此刻事务已提交，DB 中一定有这条消息
        Message message = event.getMessage();
        // 投递到 MQ，由 RabbitLister 消费并调用 WebSocket 推送
        rabbitTemplate.convertAndSend(
                "websocket.exchange",
                "sendtext",
                JSONUtil.toJsonStr(message));
    }
}
