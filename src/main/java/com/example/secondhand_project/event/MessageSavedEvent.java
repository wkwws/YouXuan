package com.example.secondhand_project.event;

import com.example.secondhand_project.pojo.entity.Message;
import lombok.Getter;

/**
 * 消息保存成功事件
 *
 * 作用：由 MessageServiceImpl.addmessage() 在事务内发布，
 *      配合 @TransactionalEventListener(AFTER_COMMIT)，
 *      把"投递 RabbitMQ"延迟到事务提交成功之后执行，
 *      避免"数据库回滚但消息已推送"的脏数据问题。
 */
@Getter
public class MessageSavedEvent {

    /** 刚保存到数据库的消息对象 */
    private final Message message;

    public MessageSavedEvent(Message message) {
        this.message = message;
    }
}
