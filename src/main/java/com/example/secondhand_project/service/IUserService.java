package com.example.secondhand_project.service;

import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.entity.Product;
import com.example.secondhand_project.pojo.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.PostVO;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author wkw
 * @since 2026-05-31
 */
public interface IUserService extends IService<User> {

    User getuserinfo();

    PageVO<Product> getuserproduct(Pagee pagee);

    void updateuserifo(User user);

    PageVO<PostVO> getuserpost(Pagee pagee);
}
