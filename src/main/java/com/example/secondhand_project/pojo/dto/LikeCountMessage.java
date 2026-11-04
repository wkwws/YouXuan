package com.example.secondhand_project.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 点赞计数 MQ 消息体。
 *
 * <p>
 * 模板 tj-remark 直接发送 List&lt;LikedTimesDTO&gt;，由业务服务按 routingKey 区分业务类型。
 * 这里额外包一层 bizType，方便单队列消费时也能识别类型，后续加评论点赞时无需改消息结构。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeCountMessage {

    /**
     * 业务类型：POST=说说
     */
    private String bizType;

    /**
     * 一批业务对象的点赞总数
     */
    private List<LikedTimesDTO> list;

}
