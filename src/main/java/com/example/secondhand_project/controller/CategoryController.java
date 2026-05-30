package com.example.secondhand_project.controller;


import com.example.secondhand_project.pojo.vo.CategoryVO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 商品分类表 前端控制器
 * </p>
 *
 * @author wkw
 * @since 2026-05-29
 */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private ICategoryService categoryService;

    /**
     * 获取分类列表
     * @return
     */
    @GetMapping()
    public Result getlist(){
       List<CategoryVO> vo= categoryService.getlist();
        return Result.success(vo);
    }

}
