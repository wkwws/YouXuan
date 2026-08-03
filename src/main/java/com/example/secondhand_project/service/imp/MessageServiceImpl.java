package com.example.secondhand_project.service.imp;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.event.MessageSavedEvent;
import com.example.secondhand_project.mapper.UserMapper;
import com.example.secondhand_project.pojo.dto.ConvId;
import com.example.secondhand_project.pojo.dto.MessageDTO;
import com.example.secondhand_project.pojo.entity.Conversation;
import com.example.secondhand_project.pojo.entity.Message;
import com.example.secondhand_project.mapper.MessageMapper;
import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.pojo.vo.ConversationVO;
import com.example.secondhand_project.pojo.vo.MessageVO;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.UpdatedCount;
import com.example.secondhand_project.service.IConversationService;
import com.example.secondhand_project.service.IMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secondhand_project.service.IUserService;
import com.example.secondhand_project.websocketserver.WebSocketServer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 消息记录表 服务实现类
 * </p>
 *
 * @author wkw
 * @since 2026-06-02
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {


    @Autowired
    private IConversationService conversationService;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** 事件发布器：用于发布"消息已保存"事件，事务提交后由监听器投递 MQ */
    @Autowired
    private ApplicationEventPublisher publisher;


    @Override
    @Transactional
    public MessageVO addmessage(MessageDTO messageDTO, String userId) {
        if(messageDTO.getImage()==null){
            messageDTO.setImage("");
        }
        //首先创建conversation，利用save导入数据库， 主要是要使用conversation_id
        Conversation conversation=new Conversation();
        Long userid1 = BaseContext.getCurrentId();
        //Long userid1=2L;
        //首先判断user1和user2是否已存在会话，如果存在直接调用就行了
        Conversation one2 = conversationService.lambdaQuery().eq(Conversation::getUid1, userid1)
                .eq(Conversation::getUid2, userId)
                .one();
        if(one2!=null){
            //有则直接复制
            BeanUtils.copyProperties(one2,conversation);
            //并更新最后的消息发出的时间 和 信息
            Conversation one = conversationService.lambdaQuery()
                    .eq(Conversation::getUid1, userId)
                    .eq(Conversation::getUid2, userid1)
                    .one();
            one.setLastTime(LocalDateTime.now());
            one.setLastMsg(JSONUtil.toJsonStr(messageDTO));
            one.setUnreadUid1(one.getUnreadUid1()+1);
            conversationService.updateById(one);
            
            one2.setLastMsg(JSONUtil.toJsonStr(messageDTO));
            one2.setLastTime(LocalDateTime.now());
            one2.setUnreadUid2(one.getUnreadUid2()+1);
            conversationService.updateById(one2);

        }else{
            //无则创建一个
             //fromuser->touser
            conversation.setUid1(Integer.valueOf(userid1.toString()));
            conversation.setUid2(Integer.valueOf(userId));
            conversation.setLastTime(LocalDateTime.now());
            conversation.setLastMsg(JSONUtil.toJsonStr(messageDTO));
            conversation.setUnreadUid2(1);
            conversationService.save(conversation);
             //touser->fromuser
            Conversation c=new Conversation();
            c.setLastMsg(JSONUtil.toJsonStr(messageDTO));
            c.setLastTime(LocalDateTime.now());
            c.setUid1(conversation.getUid2());
            c.setUid2(conversation.getUid1());
            c.setUnreadUid1(1);
            conversationService.save(c);
        }
        Integer cid = conversation.getId();
        //创建将数据填充到Message中后传入数据库中
        Message message=new Message();
        message.setConversationId(cid);
        message.setContent(messageDTO.getContent());
        message.setImage((messageDTO.getImage()));
        message.setFromUserId(Integer.valueOf(userid1.toString()));
        message.setToUserId(conversation.getUid2());
        message.setCreatedAt(LocalDateTime.now());
        this.save(message);
        //调用websocket方法，将消息推送的对方用户
           //这里我们使用 异步 出入 rabbitmq 中
        //
        // ============================================================
        // 【隐患1 修复方案】
        // 旧写法：事务内直接投递 MQ
        //   @Transactional 只管数据库回滚，管不到 RabbitMQ；
        //   若投递后事务回滚，会出现"推送了但库里没记录"的脏数据。
        //
        // 新写法：发布事件，真正投递 MQ 的动作延后到【事务提交成功后】
        //   由 MessagePushListener(@TransactionalEventListener AFTER_COMMIT) 执行，
        //   保证"数据库里有，才推送"。
        // ============================================================
        // 旧写法（注释保留，供对照）：
        // String exchange="websocket.exchange";
        // rabbitTemplate.convertAndSend(exchange,"sendtext",JSONUtil.toJsonStr(message));
        // String jsonString = JSONUtil.toJsonStr(message);
        // webSocketServer.sendToClient(userId,jsonString);

        // 新写法：发布事件（事务提交成功后由监听器投递 MQ）
        publisher.publishEvent(new MessageSavedEvent(message));


        //构造MessageVO 返回值
          //fromuser信息
        User one = Db.lambdaQuery(User.class)
                .eq(User::getId, userid1).one();
          //touser信息
        User one1 = Db.lambdaQuery(User.class)
                .eq(User::getId, userId).one();

        MessageVO messageVO=new MessageVO();
        messageVO.setId(message.getId());
        messageVO.setConversationId(cid);
        messageVO.setFromUserId(Integer.valueOf(userid1.toString()));
        messageVO.setToUserId(Integer.valueOf(userId));
        messageVO.setFromUsername(one.getUsername());
        messageVO.setFromAvatar(one.getAvatar());
        messageVO.setContent(messageDTO.getContent());
        messageVO.setImage(messageDTO.getImage());
        messageVO.setMsgType("text");
        messageVO.setStatus(0);
        messageVO.setCreateAt(LocalDateTime.now());

        return messageVO;
    }

    @Override
    public PageVO getmessagelist(Pagee pagee, String userId) {
        //构建分页查询所用的参数
       Long currentId = BaseContext.getCurrentId();
          //Long currentId=2L;
        Page<Message> page=new Page<>(1,5000);
        Page<Message> page1 = this.lambdaQuery()
                .in(Message::getToUserId, userId, currentId)
                .in(Message::getFromUserId, currentId,userId)
                .orderByAsc(Message::getCreatedAt)
                .page(page);

        //通过stream流批量beanUtil.copy
        List<Message> records = page1.getRecords();
        List<MessageVO> collect = records.stream().map(message -> {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);
            //获取fromuser 资料
            User one = Db.lambdaQuery(User.class)
                    .eq(User::getId, message.getFromUserId())
                    .one();
            //获取touser  资料
            User one1 = Db.lambdaQuery(User.class)
                    .eq(User::getId, message.getToUserId()).one();
            messageVO.setFromUsername(one.getUsername());
            messageVO.setFromAvatar(one.getAvatar());
            messageVO.setCreateAt(message.getCreatedAt());
            return messageVO;
        }).collect(Collectors.toList());

        //封装pageVO

        PageVO pageVO = new PageVO();
        pageVO.setPageSize(pagee.getPageSize());
        pageVO.setTotal((int) page1.getTotal());
        pageVO.setList(collect);
        pageVO.setPage(pagee.getPage());

        return pageVO;
    }

    @Override
    public List<ConversationVO> getmessage() {
        //获取会话列表

        Long currentId = BaseContext.getCurrentId();
         // Long currentId=2L;
        List<Conversation> list = conversationService.lambdaQuery().eq(Conversation::getUid1, currentId).list();

        //通过stream一键转换加组装
        List<ConversationVO> collect = list.stream().map(conversation -> {
            User one = Db.lambdaQuery(User.class)
                    .eq(User::getId, conversation.getUid2()).one();
            ConversationVO conversationVO = new ConversationVO();
            conversationVO.setUserId(conversation.getUid2());
            conversationVO.setUsername(one.getUsername());
            conversationVO.setNickname(one.getNickname());
            conversationVO.setAvatar(one.getAvatar());
            conversationVO.setConversationId(conversation.getId());
            String lastMsg = conversation.getLastMsg();
            //处理前端json未处理
            JSONObject entries = JSONUtil.parseObj(lastMsg);
            if(entries!=null){
                String content = entries.get("content").toString();
                String image = entries.get("image").toString();
                if(content.isEmpty()&&!image.isEmpty()){
                    conversationVO.setLastMessage("[图片]");
                }else if(!content.isEmpty()&&image.isEmpty()){
                    conversationVO.setLastMessage(content);
                }else{
                    conversationVO.setLastMessage(content);
                }
            }
            conversationVO.setLastTime(conversation.getLastTime());
            conversationVO.setUnread(conversation.getUnreadUid1());
            return conversationVO;
        }).collect(Collectors.toList());

        return collect;
    }

    @Override
    public UpdatedCount updatemessage(ConvId convId) {
        //将会话列表化为0

        Conversation one = conversationService.lambdaQuery()
                .eq(Conversation::getId, convId.getConvId())
                .one();
        Integer unreadUid1 = one.getUnreadUid1();
        conversationService.lambdaUpdate()
                .eq(Conversation::getId, convId.getConvId())
                .set(Conversation::getUnreadUid1,0).update();
        Integer uid2 = one.getUid2();
        conversationService.lambdaUpdate()
                .eq(Conversation::getUid2,one.getUid1())
                .eq(Conversation::getUid1,uid2)
                .set(Conversation::getUnreadUid2,0).update();

        //同时将信息状态化为2（已读）

        UpdatedCount updatedCount = new UpdatedCount();
        updatedCount.setUpdatedCount(unreadUid1);

        return updatedCount;
    }
}
