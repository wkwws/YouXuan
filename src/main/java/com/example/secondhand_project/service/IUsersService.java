package com.example.secondhand_project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.PostVO;
import com.example.secondhand_project.pojo.vo.ProductVO;

public interface IUsersService extends IService<User> {
    PageVO<ProductVO> getusersproduct(Integer uid, Pagee pagee);

    PageVO<PostVO> getuserpost(Integer uid, Pagee pagee);
}
