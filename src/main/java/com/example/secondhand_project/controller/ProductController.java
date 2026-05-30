package com.example.secondhand_project.controller;


import com.example.secondhand_project.pojo.dto.ProductDTO;
import com.example.secondhand_project.pojo.entity.Product;
import com.example.secondhand_project.pojo.vo.ProductVO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 商品表 前端控制器
 * </p>
 *
 * @author wkw
 * @since 2026-05-29
 */
@RestController
@RequestMapping("/products")
public class ProductController {


    @Autowired
    private IProductService iProductService;


    /**
     *添加商品
     * @param productDTO
     * @return
     */
    @PostMapping()
    public Result addProduct(@RequestBody ProductDTO productDTO){

        ProductVO productVO = iProductService.addProduct(productDTO);

        return Result.success(productVO);
    }

}
