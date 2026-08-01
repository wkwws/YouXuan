package com.example.secondhand_project.service;

import com.example.secondhand_project.pojo.dto.PostDTO;
import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.entity.Post;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.PostVO;

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

    PageVO<PostVO> getpost(Pagee pagee);

    void deletepost(int id);
}
