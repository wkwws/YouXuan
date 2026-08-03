package com.example.secondhand_project.listener;


import cn.hutool.json.JSONUtil;
import com.example.secondhand_project.pojo.entity.Message;
import com.example.secondhand_project.websocketserver.WebSocketServer;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RabbitLister {


    @Autowired
    private WebSocketServer webSocketServer;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "websocket.queue",durable = "true"),
            exchange = @Exchange(name = "websocket.exchange"),
            key = "sendtext"
    ))
    public void listen(String message){

        Message bean = JSONUtil.toBean(message, Message.class);
        Integer toUserId = bean.getToUserId();

        System.out.println(message);

        webSocketServer.sendToClient(String.valueOf(toUserId),message);


    }

}
