package com.example.secondhand_project.service.imp;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secondhand_project.mapper.UserMapper;
import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.entity.Post;
import com.example.secondhand_project.pojo.entity.Product;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.PostVO;
import com.example.secondhand_project.pojo.vo.ProductVO;
import com.example.secondhand_project.service.IPostService;
import com.example.secondhand_project.service.IProductService;
import com.example.secondhand_project.service.IUsersService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsersServiceImpl extends ServiceImpl<UserMapper, User> implements IUsersService {


    @Autowired
    private IProductService iProductService;
    @Autowired
    private IPostService  iPostService;

    @Override
    public PageVO<ProductVO> getusersproduct(Integer uid, Pagee pagee) {

        Page<Product> page = new Page<>(pagee.getPage(), pagee.getPageSize());
        Page<Product> page1 = iProductService.lambdaQuery()
                .eq(Product::getUid, uid)
                .page(page);

        List<Product> records = page1.getRecords();

        List<ProductVO> collect = records.stream().map(product -> {
            ProductVO build = ProductVO.builder().build();
            BeanUtils.copyProperties(product, build);
            return build;
        }).collect(Collectors.toList());
        PageVO<ProductVO> pageVO = new PageVO<>();
        pageVO.setPageSize(pagee.getPageSize());
        pageVO.setPage(pagee.getPage());
        pageVO.setList(collect);
        pageVO.setTotal((int) page1.getTotal());

        return pageVO;
    }

    @Override
    public PageVO<PostVO> getuserpost(Integer uid, Pagee pagee) {

        Page<Post> page = new Page<>(pagee.getPage(), pagee.getPageSize());

        Page<Post> page1 = iPostService.lambdaQuery()
                .eq(Post::getUid, uid)
                .page(page);
        List<Post> records = page1.getRecords();
        List<PostVO> collect = records.stream().map(post -> {
            PostVO build = PostVO.builder().build();
            BeanUtils.copyProperties(post, build);
            return build;
        }).collect(Collectors.toList());

        PageVO<PostVO> pageVO = new PageVO<>();
        pageVO.setPageSize(pagee.getPageSize());
        pageVO.setPage(pagee.getPage());
        pageVO.setList(collect);
        pageVO.setTotal((int) page1.getTotal());

        return pageVO;
    }
}