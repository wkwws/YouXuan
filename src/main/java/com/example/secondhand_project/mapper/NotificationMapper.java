package com.example.secondhand_project.mapper; // 声明 Mapper 接口所在包

import com.baomidou.mybatisplus.core.mapper.BaseMapper; // 提供 MyBatis-Plus 通用 CRUD 能力
import com.example.secondhand_project.pojo.entity.Notification; // 引入通知实体

public interface NotificationMapper extends BaseMapper<Notification> { // 通知表 Mapper，继承通用 BaseMapper
}
