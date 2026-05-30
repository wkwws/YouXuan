package com.example.secondhand_project.service;

import com.example.secondhand_project.pojo.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.secondhand_project.pojo.vo.CategoryVO;

import java.util.List;

/**
 * <p>
 * 商品分类表 服务类
 * </p>
 *
 * @author wkw
 * @since 2026-05-29
 */
public interface ICategoryService extends IService<Category> {

    List<CategoryVO> getlist();
}
