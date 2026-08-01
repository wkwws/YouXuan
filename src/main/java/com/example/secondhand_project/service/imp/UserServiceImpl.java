package com.example.secondhand_project.service.imp;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.entity.Post;
import com.example.secondhand_project.pojo.entity.Product;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.mapper.UserMapper;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.PostVO;
import com.example.secondhand_project.service.IProductService;
import com.example.secondhand_project.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author wkw
 * @since 2026-05-31
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private IProductService iProductService;

    @Override
    public User getuserinfo() {

        Long currentId = BaseContext.getCurrentId();

        User byId = this.getById(currentId);

        return byId;
    }

    @Override
    public PageVO<Product> getuserproduct(Pagee pagee) {
        //获取当前用户的id
        Long currentId = BaseContext.getCurrentId();
        //根据id查询商品
          //设置分页器
        Page<Product> pag=new Page<>(pagee.getPage(),pagee.getPageSize());

        Page<Product> page = iProductService.lambdaQuery()
                .eq(currentId!=null&&currentId!=0, Product::getUid, currentId).page(pag);

        List<Product> records = page.getRecords();
        //封装成pageVO  注意bulider 和 new不能同时使用
        PageVO<Product> pageVO = new PageVO<>();
        pageVO.setPageSize(pagee.getPageSize());
        pageVO.setList(records);
        pageVO.setTotal((int) page.getTotal());
        pageVO.setPage(pagee.getPage());


        return pageVO;
    }

    @Override
    public void updateuserifo(User user) {

        //从threadlocal中获取用户的id
        Long currentId = BaseContext.getCurrentId();

        //将id添加到user中 ,更新时间
        user.setId(((Number)currentId).intValue());
        user.setUpdatedAt(LocalDateTime.now());

        //利用server的便捷方式进行更新
        this.updateById(user);

    }

    @Override
    public PageVO<PostVO> getuserpost(Pagee pagee) {

        Page<Post> pag=new Page<>(pagee.getPage(),pagee.getPageSize());

        //调用Db的lambda处理，防止注入循环
        Page<Post> page = Db.lambdaQuery(Post.class)
                .eq(Post::getUid, BaseContext.getCurrentId())
                .page(pag);


        List<PostVO> collect = page.getRecords().stream().map(post -> {
            PostVO build = PostVO.builder().build();
            BeanUtils.copyProperties(post, build);
            return build;
        }).collect(Collectors.toList());

        PageVO<PostVO> pageVO = new PageVO<>();
        pageVO.setPageSize(pagee.getPageSize());
        pageVO.setList(collect);
        pageVO.setTotal((int) page.getTotal());
        pageVO.setPage(pagee.getPage());

        return pageVO;
    }
}
