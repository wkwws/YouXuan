package com.example.secondhand_project.controller;

import com.example.secondhand_project.annotation.RateLimit;
import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.entity.Product;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.PostVO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "03-用户中心", description = "当前登录用户的个人信息管理")
public class UserController {


    @Autowired
    private IUserService userService;

    /**
     * 获取当前用户详细信息
     * @return
     */
    @Operation(summary = "获取当前用户详细信息", description = "获取当前登录用户的详细资料（需登录）")
    @GetMapping("/profile")
    public Result getuserinfo(){

       User uu=  userService.getuserinfo();

        return Result.success(uu);
    }


    /**
     * 获取当前用户商品
     * @return
     */
    @Operation(summary = "获取当前用户的商品", description = "分页获取当前登录用户发布的商品列表（需登录）")
    @GetMapping("/products")
    public Result getuserproduct(Pagee pagee){

       PageVO<Product> p= userService.getuserproduct(pagee);

        return Result.success(p);
    }


    /**
     * 更新用户信息
     * @return
     */
    @Operation(summary = "更新当前用户信息", description = "更新当前登录用户的个人资料（需登录）")
    @RateLimit(count = 5, window = 60, key = "user:update-profile",
            message = "操作过于频繁，请稍后再试")
    @PutMapping("/profile")
    public Result updateuserinfo(@RequestBody User user){

        userService.updateuserifo(user);

        return Result.success("更新资料成功");
    }


    @Operation(summary = "获取当前用户的说说", description = "分页获取当前登录用户发布的校园说说列表（需登录）")
    @GetMapping("/posts")
    public Result getuserpost(Pagee pagee){

        PageVO<PostVO> pp=userService.getuserpost(pagee);

        return Result.success(pp);
    }


}
