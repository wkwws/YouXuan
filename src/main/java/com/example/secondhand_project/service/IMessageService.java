package com.example.secondhand_project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.secondhand_project.pojo.dto.ConvId;
import com.example.secondhand_project.pojo.dto.MessageDTO;
import com.example.secondhand_project.pojo.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.vo.ConversationVO;
import com.example.secondhand_project.pojo.vo.MessageVO;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.UpdatedCount;

import java.util.List;

/**
 * <p>
 * 消息记录表 服务类
 * </p>
 *
 * @author wkw
 * @since 2026-06-02
 */
public interface IMessageService extends IService<Message> {

    MessageVO addmessage(MessageDTO messageDTO, String userId);

    PageVO getmessagelist(Pagee page, String userId);

    List<ConversationVO> getmessage();

    UpdatedCount updatemessage(ConvId convId);
}
