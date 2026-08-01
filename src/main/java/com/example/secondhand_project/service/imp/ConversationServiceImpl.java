package com.example.secondhand_project.service.imp;

import com.example.secondhand_project.pojo.entity.Conversation;
import com.example.secondhand_project.mapper.ConversationMapper;
import com.example.secondhand_project.service.IConversationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 私信会话表 服务实现类
 * </p>
 *
 * @author wkw
 * @since 2026-06-02
 */
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements IConversationService {

}
