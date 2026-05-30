package com.example.secondhand_project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.secondhand_project.pojo.dto.LoginDTO;
import com.example.secondhand_project.pojo.entity.User;

public interface AuthService  extends IService<User> {
    User login(LoginDTO loginDTO);

    void register(LoginDTO user);


}
