package com.example.secondhand_project.service; // 声明服务接口所在包

import com.baomidou.mybatisplus.extension.service.IService; // 提供 MyBatis-Plus 通用服务能力
import com.example.secondhand_project.pojo.entity.Comment; // 一级评论实体
import com.example.secondhand_project.pojo.entity.Notification; // 通知实体
import com.example.secondhand_project.pojo.entity.Post; // 说说实体
import com.example.secondhand_project.pojo.entity.SubComment; // 子评论实体
import com.example.secondhand_project.pojo.vo.NotificationVO; // 通知返回对象
import com.example.secondhand_project.pojo.vo.PageVO; // 通用分页返回对象

/**
 * 互动通知服务。
 */
public interface INotificationService extends IService<Notification> { // 通知服务接口，继承通用 CRUD

    PageVO<NotificationVO> pageNotifications(int page, int pageSize); // 分页查询当前用户的通知

    int unreadCount(); // 查询当前用户的未读通知数

    void markRead(long id); // 将当前用户的一条通知标记为已读

    void markAllRead(); // 将当前用户的全部通知标记为已读

    void notifyNewComment(Post post, Comment comment); // 新增一级评论后生成通知

    void notifyNewReply(Post post, Comment comment, SubComment reply); // 新增回复后生成通知
}
