package com.example.secondhand_project.service.imp;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.pojo.dto.ProductDTO;
import com.example.secondhand_project.pojo.entity.Product;
import com.example.secondhand_project.mapper.ProductMapper;
import com.example.secondhand_project.pojo.vo.ProductVO;
import com.example.secondhand_project.service.IProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author wkw
 * @since 2026-05-29
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {


    @Autowired
    private ProductMapper productMapper;


    @Override
    public ProductVO addProduct(ProductDTO productDTO) {

        Long currentId = BaseContext.getCurrentId();

        Product Product = new Product();
        BeanUtils.copyProperties(productDTO,Product);

        Product product = Product.setUid(Math.toIntExact(currentId));

        this.save(Product);

        Product byId = this.getById(Product.getId());


        ProductVO build = ProductVO.builder()
                .id(Product.getId())
                .title(Product.getTitle())
                .price(Product.getPrice())
                .createdAt(LocalDateTime.now())
                .build();

        return build;

    }
}
