package com.example.secondhand_project.service;

import com.example.secondhand_project.pojo.dto.PostDTO;
import com.example.secondhand_project.pojo.entity.Post;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.secondhand_project.pojo.vo.PostVO;

import java.util.List;

/**
 * <p>
 * 说说表（校园生活圈） 服务类
 * </p>
 *
 * @author wkw
 * @since 2026-05-31
 */
public interface IPostService extends IService<Post> {

    PostVO addpost(PostDTO post);

    /**
     * 大厅：一次返回最新 10 条说说。
     */
    List<PostVO> listLatest();

    void deletepost(int id);

    /**
     * 失效大厅缓存。新增/删除说说、评论数变化、点赞计数落库后调用。
     */
    void evictFeedCache();
}
