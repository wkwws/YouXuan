package com.example.secondhand_project.service; // 服务接口所在的包

import com.example.secondhand_project.pojo.dto.PostDTO; // 发布说说的请求体
import com.example.secondhand_project.pojo.entity.Post; // 说说实体
import com.baomidou.mybatisplus.extension.service.IService; // MyBatis-Plus 基础服务
import com.example.secondhand_project.pojo.vo.PostVO; // 返回给前端的说说

/**
 * <p>
 * 说说表（校园生活圈） 服务类
 * </p>
 *
 * @author wkw
 * @since 2026-05-31
 */
public interface IPostService extends IService<Post> { // 继承基础 CRUD

    PostVO addpost(PostDTO post); // 发布说说，返回新说说

    void deletepost(int id); // 删除说说

    /**
     * 失效单条说说的 Feed 快照。
     *
     * <p>点赞数/评论数变化后调用；随机池的权重会在下一次定时重建时更新。</p>
     */
    void evictFeedPost(int postId); // 只删这条帖子的内容缓存
}
