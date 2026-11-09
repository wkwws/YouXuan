package com.example.secondhand_project.service; // 服务接口所在的包

import com.example.secondhand_project.pojo.vo.PostVO; // Feed 返回的说说对象

import java.util.List; // 返回多条说说

/**
 * 说说大厅随机 Feed。
 */
public interface IFeedService {

    /**
     * 随机返回说说。
     *
     * @param size    期望条数
     * @param exclude 当前这批的 ID，用于“换一批”尽量不重复
     */
    List<PostVO> randomFeed(int size, List<Integer> exclude); // 随机取一页，再把 ID 变成完整说说
}
