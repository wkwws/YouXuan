package com.example.secondhand_project.controller; // 声明控制器所在包

import com.example.secondhand_project.pojo.vo.NotificationVO; // 通知返回对象
import com.example.secondhand_project.pojo.vo.PageVO; // 分页返回对象
import com.example.secondhand_project.result.Result; // 统一接口返回结构
import com.example.secondhand_project.service.INotificationService; // 通知业务服务
import io.swagger.v3.oas.annotations.Operation; // Swagger 接口说明
import io.swagger.v3.oas.annotations.tags.Tag; // Swagger 分组说明
import lombok.RequiredArgsConstructor; // 自动生成构造器注入 final 字段
import org.springframework.web.bind.annotation.GetMapping; // 声明 GET 接口
import org.springframework.web.bind.annotation.PathVariable; // 读取路径参数
import org.springframework.web.bind.annotation.PostMapping; // 声明 POST 接口
import org.springframework.web.bind.annotation.RequestMapping; // 声明控制器统一路径
import org.springframework.web.bind.annotation.RequestParam; // 读取查询参数
import org.springframework.web.bind.annotation.RestController; // 声明 REST 控制器

/**
 * 评论/回复的互动通知。
 */
@RestController // 返回 JSON 的控制器
@RequestMapping("/notifications") // 当前控制器下所有接口以 /notifications 开头
@RequiredArgsConstructor // 通过构造器注入 notificationService
@Tag(name = "08-互动通知", description = "评论、回复通知的查询与已读") // Swagger 接口分组
public class NotificationController { // 互动通知控制器

    private final INotificationService notificationService; // 通知业务服务

    @Operation(summary = "分页查询互动通知") // Swagger 接口说明
    @GetMapping // 处理 GET /notifications
    public Result<PageVO<NotificationVO>> page( // 返回当前用户的分页通知列表
            @RequestParam(defaultValue = "1") int page, // 页码，默认第 1 页
            @RequestParam(defaultValue = "20") int pageSize) { // 每页条数，默认 20
        return Result.success(notificationService.pageNotifications(page, pageSize)); // 调用服务并包装为统一成功结果
    }

    @Operation(summary = "查询未读通知数") // Swagger 接口说明
    @GetMapping("/unread-count") // 处理 GET /notifications/unread-count
    public Result<Integer> unreadCount() { // 返回当前用户未读通知数量
        return Result.success(notificationService.unreadCount()); // 调用服务并包装为统一成功结果
    }

    @Operation(summary = "将一条通知标记为已读") // Swagger 接口说明
    @PostMapping("/{id}/read") // 处理 POST /notifications/{id}/read
    public Result<String> markRead(@PathVariable long id) { // 路径中的通知 ID
        notificationService.markRead(id); // 只允许修改当前用户自己的通知
        return Result.success("已读"); // 返回成功提示
    }

    @Operation(summary = "全部标记为已读") // Swagger 接口说明
    @PostMapping("/read-all") // 处理 POST /notifications/read-all
    public Result<String> markAllRead() { // 将当前用户的全部通知标记为已读
        notificationService.markAllRead(); // 调用服务批量更新
        return Result.success("已读"); // 返回成功提示
    }
}
