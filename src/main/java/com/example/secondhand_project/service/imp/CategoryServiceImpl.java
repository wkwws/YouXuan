package com.example.secondhand_project.service.imp;

import com.example.secondhand_project.pojo.entity.Category;
import com.example.secondhand_project.mapper.CategoryMapper;
import com.example.secondhand_project.pojo.vo.CategoryVO;
import com.example.secondhand_project.service.ICategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 商品分类表 服务实现类
 * </p>
 *
 * @author wkw
 * @since 2026-05-29
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {




    @Override
    public List<CategoryVO> getlist() {

        List<Category> list = this.lambdaQuery().list();

        List<CategoryVO> categoryVOList = new ArrayList<>();

        //通过stream实现类型的转换
        List<CategoryVO> collect = list.stream().map(category -> {
            CategoryVO categoryVO = new CategoryVO();
            BeanUtils.copyProperties(category, categoryVO);
            return categoryVO;
        }).collect(Collectors.toList());

        return collect;
    }

}
