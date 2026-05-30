package com.example.secondhand_project.controller;

import com.example.secondhand_project.conf.JwtConf;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.pojo.dto.LoginDTO;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.pojo.vo.LoginVO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.AuthService;
import com.example.secondhand_project.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/auth")
public class AuthController {


    @Autowired
    private AuthService authService;
    @Autowired
    private JwtConf  jwtConf;

    /**
     * 登录操作
     * @param loginDTO
     * @return
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO loginDTO) {
        //密码的判断并返回User实体类对象
        User user=authService.login(loginDTO);
        //准备数据
        Map<String,Long> clams=new HashMap<>();
        clams.put("id", Long.valueOf(user.getId()));
        String secretKey = jwtConf.getSecretKey();
        long ttlMills = jwtConf.getTtlMills();
        //生成jwt码
       String token= JwtUtil.createJWT(jwtConf.getSecretKey(),jwtConf.getTtlMills(),clams);
        //封装vo数据
        LoginVO l=LoginVO.builder()
                .user(user)
                .Token(token)
                .build();
        //返回数据
        log.info("token:{}",token);
        return Result.success(l);
    }


    /**
     * 注册操作
     * @param loginDTO
     * @return
     */
    @PostMapping("/register")
    public Result register(@RequestBody LoginDTO loginDTO) {

        authService.register(loginDTO);

        return Result.success();
    }


    /**
     * 获取用户信息
     *
     */

    @GetMapping("/me")
    public Result me(){

        Long currentId = BaseContext.getCurrentId();

        User byId = authService.getById(currentId);

        return  Result.success(byId);
    }

    @PostMapping("/logout")
    public Result logout(){


        return Result.success("退出登录成功");
    }
}
