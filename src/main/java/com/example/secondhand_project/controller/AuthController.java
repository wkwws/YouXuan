package com.example.secondhand_project.controller;

import com.example.secondhand_project.annotation.RateLimit;
import com.example.secondhand_project.conf.JwtConf;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.enums.RateLimitDimension;
import com.example.secondhand_project.pojo.dto.LoginDTO;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.pojo.vo.LoginVO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.AuthService;
import com.example.secondhand_project.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/auth")
@Tag(name = "01-认证管理", description = "登录、注册、获取用户信息、登出")
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
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT Token 和用户信息")
    @RateLimit(count = 10, window = 60, dimension = RateLimitDimension.IP,
            key = "auth:login", message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO loginDTO) {
        //密码的判断并返回User实体类对象
        User user=authService.login(loginDTO);
        //准备数据
        Map<String, Object> clams=new HashMap<>();
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
        return Result.success(l);
    }
    /**
     * 注册操作
     * @param loginDTO
     * @return
     */
    @Operation(summary = "用户注册", description = "注册新用户，用户名和密码必填")
    @RateLimit(count = 5, window = 300, dimension = RateLimitDimension.IP,
            key = "auth:register", message = "注册过于频繁，请稍后再试")
    @PostMapping("/register")
    public Result register(@RequestBody LoginDTO loginDTO) {

        authService.register(loginDTO);

        return Result.success();
    }


    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "根据 JWT Token 获取当前登录用户的详细信息（需登录）")
    @GetMapping("/me")
    public Result me(){

        Long currentId = BaseContext.getCurrentId();

        User byId = authService.getById(currentId);

        return  Result.success(byId);
    }

    /**
     * 登出
     * @return
     */
    @Operation(summary = "退出登录", description = "退出当前登录（Token 由前端清除）")
    @PostMapping("/logout")
    public Result logout(){
        return Result.success("退出登录成功");
    }
}
