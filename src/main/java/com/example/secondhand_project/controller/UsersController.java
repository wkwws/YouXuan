package com.example.secondhand_project.controller;


import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.PostVO;
import com.example.secondhand_project.pojo.vo.ProductVO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.IProductService;
import com.example.secondhand_project.service.IUsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "07-用户公开信息", description = "查看其他用户的公开信息、商品和说说")
public class UsersController {


    @Autowired
    private IUsersService iUsersService;

    @Autowired
    private IProductService iProductService;

    /**
     * 获取他人的个人信息
     * @param uid
     * @return
     */
    @Operation(summary = "获取他人信息", description = "根据用户 ID 查看指定用户的公开个人资料")
    @GetMapping("/{uid}/profile")
    public Result getusers(@Parameter(description = "目标用户 ID") @PathVariable Integer uid){
        User byId = iUsersService.getById(uid);
        return Result.success(byId);
    }


    /**
     * 获取他的二手商品
     * @param uid
     * @param pagee
     * @return
     */
    @Operation(summary = "获取他人的商品", description = "分页查看指定用户发布的二手商品")
    @GetMapping("/{uid}/products")
    public Result getusersproduct(@Parameter(description = "目标用户 ID") @PathVariable Integer uid, Pagee pagee){

        PageVO<ProductVO> pp=iUsersService.getusersproduct(uid,pagee);

        return Result.success(pp);
    }


    /**
     * 获取他人的说说
     * @param uid
     * @return
     */
    @Operation(summary = "获取他人的说说", description = "分页查看指定用户发布的校园说说")
    @GetMapping("/{uid}/posts")
    public Result getuserpost(@Parameter(description = "目标用户 ID") @PathVariable Integer uid,Pagee pagee){

        PageVO<PostVO> pp=iUsersService.getuserpost(uid,pagee);



        return Result.success(pp);
    }

}
