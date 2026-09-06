package com.example.secondhand_project.service.imp;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secondhand_project.exception.LoginException;
import com.example.secondhand_project.exception.NullUser;
import com.example.secondhand_project.exception.RegisterException;
import com.example.secondhand_project.mapper.AuthMapper;
import com.example.secondhand_project.pojo.dto.LoginDTO;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

@Service
public class AuthServiceImpl  extends ServiceImpl<AuthMapper,User> implements AuthService {

    @Autowired
    private AuthMapper authMapper;

    //登录操作
    @Override
    public User login(LoginDTO loginDTO) {
        //service写主要的判断流程
        String password = loginDTO.getPassword();
        String username = loginDTO.getUsername();
        if(username == null || password == null){
            throw new LoginException("空值输入");
        }
        User user=authMapper.getbyusername(loginDTO);

        if(user==null){

            throw new NullUser("用户不存在");

        }
        //对密码进行加密并继续校准
        String s = DigestUtils.md5DigestAsHex(password.getBytes());

        if(!user.getPwd().equals(s)){
            throw new LoginException("密码错误");
        }
        //如果密码正确，就可以返回值了

        return user;

    }


    //TODO 加全局异常做好前后端的交互
    @Override
    public void register(LoginDTO user) {
        //注册思路：首先判断所有参数是否都存在，虽然前端有字段拦截，但是后端还是要进行验证
        String password= user.getPassword();
        String username= user.getUsername();
        String email= user.getEmail();
        String nickname= user.getNickname();
        if(password==null||username==null||email==null){
            throw new RegisterException("信息不足");
        }
        //判断用户名是否存在
        User u=authMapper.getbyusername(user);
        if(u!=null){
            throw new RegisterException("用户名已经存在");
        }
        //将密码加密后存入数据库中

        user.setPassword( DigestUtils.md5DigestAsHex(password.getBytes()));

        User user1=new User();
        user1.setUsername(username);
        user1.setEmail(email);
        user1.setNickname(nickname);
        user1.setPwd(user.getPassword());

        //这里用到了mybatis-plus
        this.save(user1);
    }

    public void test(){


    }


}

