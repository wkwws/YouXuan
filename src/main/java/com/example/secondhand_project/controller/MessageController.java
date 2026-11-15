package com.example.secondhand_project.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.example.secondhand_project.annotation.RateLimit;
import com.example.secondhand_project.pojo.dto.ConvId;
import com.example.secondhand_project.pojo.dto.MessageDTO;
import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.vo.ConversationVO;
import com.example.secondhand_project.pojo.vo.MessageVO;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.UpdatedCount;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.IMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 消息记录表 前端控制器
 * </p>
 *
 * @author wkw
 * @since 2026-06-02
 */
@RestController
@RequestMapping("/messages")
@Tag(name = "06-消息管理", description = "私信消息的收发、会话管理")
public class MessageController {

    @Autowired
    private IMessageService iMessageService;

    /**
     * 发送消息
     * @param messageDTO
     * @param userId
     * @return
     */
    @Operation(summary = "发送消息", description = "向指定用户发送一条私信消息（需登录）")
    @RateLimit(count = 30, window = 60, key = "message:send",
            message = "发送消息过于频繁，请稍后再试")
    @PostMapping("/{userId}")
    public Result addmessage(@RequestBody MessageDTO messageDTO,
                             @Parameter(description = "接收方用户 ID") @PathVariable String userId){

        MessageVO m=iMessageService.addmessage(messageDTO,userId);

        return Result.success(m);
    }

    /**
     * 获取与某用户的聊天记录
     * @param page
     * @param userId
     * @return
     */
    @Operation(summary = "获取聊天记录", description = "分页获取与指定用户的聊天消息记录（需登录）")
    @GetMapping("/{userId}")
    public Result getmessage(Pagee page,
                             @Parameter(description = "聊天对象用户 ID") @PathVariable String userId){

        PageVO p= iMessageService.getmessagelist(page,userId);

        return Result.success(p);
    }


    /**
     * 获取会话列表
     * @return
     */
    @Operation(summary = "获取会话列表", description = "获取当前用户的所有私信会话列表（需登录）")
    @GetMapping("/conversations")
    public Result getmessagelist(){

       List<ConversationVO> c= iMessageService.getmessage();

        return Result.success(c);
    }


    /**
     * 更新信息已读
     * @param convId
     * @return
     */
    @Operation(summary = "标记消息已读", description = "将指定会话中的所有未读消息标记为已读（需登录）")
    @PostMapping("/read")
    public Result updatemessage(@RequestBody ConvId convId){
        UpdatedCount u= iMessageService.updatemessage(convId);
        return Result.success(u);
    }

}
