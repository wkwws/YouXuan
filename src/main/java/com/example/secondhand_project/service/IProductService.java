package com.example.secondhand_project.service;

import com.example.secondhand_project.pojo.dto.ProductDTO;
import com.example.secondhand_project.pojo.entity.Product;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.secondhand_project.pojo.vo.ProductVO;

/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author wkw
 * @since 2026-05-29
 */
public interface IProductService extends IService<Product> {

    ProductVO addProduct(ProductDTO productDTO);
}
