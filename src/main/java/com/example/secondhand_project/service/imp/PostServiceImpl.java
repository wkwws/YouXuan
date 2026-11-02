package com.example.secondhand_project.service.imp;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.secondhand_project.constants.RedisConstants;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.pojo.dto.PostDTO;
import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.entity.Post;
import com.example.secondhand_project.mapper.PostMapper;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.PostVO;
import com.example.secondhand_project.service.IPostService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secondhand_project.service.IUserService;
import com.example.secondhand_project.util.RedislockUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.secondhand_project.constants.RedisConstants.Post_Key;
import static com.example.secondhand_project.constants.RedisConstants.Post_lock;

/**
 * <p>
 * 说说表（校园生活圈） 服务实现类
 * </p>
 *
 * @author wkw
 * @since 2026-05-31
 */
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {

    @Autowired
    private IUserService iUserService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;



    @Override
    public PostVO addpost(PostDTO post) {
        /**
         * 添加操作看起来只需要将将对应的页数删除就行了，但是是实际上由于时间的降序排列，需要将整个post部分都进行删除
         * 这样肯定会面临并发问题，我的解决思路式加互斥锁，这样的好处是解决了并发问题，但是可能会变慢。
         */

        Post post1=new Post();

        BeanUtils.copyProperties(post,post1);

        Long currentId = BaseContext.getCurrentId();

        post1.setUid(((Number)currentId).intValue());

        this.save(post1);

        // 读取端 key 为 RedisConstants.Post_Key + page，这里按前缀统一失效
        Set<String> keys = stringRedisTemplate.keys(RedisConstants.Post_Key + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }

        User byId = iUserService.getById(((Number) currentId).intValue());


        PostVO build = PostVO.builder()
                .id(post1.getId())
                .uid(post1.getUid())
                .username(byId.getUsername())
                .nickname(byId.getNickname())
                .avatar(byId.getAvatar())
                .content(post1.getContent())
                .images(post1.getImages())
                .createdAt(LocalDateTime.now())
                .build();

        return build;


    }


    @Override
    public PageVO<PostVO> getpost(Pagee pagee) {

        //redis 优化方案
        /**
         * 先从redis中获取指定列表，
         */

        //设计随机过期时间
        long l = RandomUtil.randomLong(30);

        pagee.setPage(pagee.getPage()+1);


             String s=(stringRedisTemplate.opsForValue().get(RedisConstants.Post_Key + pagee.getPage()));


        if(s==null){
            //利用分布式锁来解决多线程问题
            boolean getlock = RedislockUtil.getlock(Post_lock+pagee.getPage());
            if(!getlock){
                PageVO<PostVO> pageVO = getPageVO(pagee);
                //如果获取redis 值失败了，就返回递归
                if(pageVO==null){
                    return getpost(pagee);
                }
                return pageVO;
            }
            try{
                //成功就进行插叙操作

                //生成分页参数
                //与前端进行拟合
                Page<Post> pp= new  Page<>(pagee.getPage(),pagee.getPageSize());

                Page<Post> page = this.lambdaQuery()
                        .orderByDesc(Post::getCreatedAt)
                        .page(pp);

                List<Post> records = page.getRecords();

                List<PostVO> collect = records.stream().map(post -> {

                    PostVO build = PostVO.builder().build();
                    BeanUtils.copyProperties(post, build);
                    User byId = iUserService.getById(post.getUid());
                    build.setUsername(byId.getUsername());
                    build.setNickname(byId.getNickname());
                    build.setAvatar(byId.getAvatar());
                    return build;
                }).collect(Collectors.toList());


                PageVO<PostVO> pageVO = new PageVO<>();
                pageVO.setTotal((int) page.getTotal());
                pageVO.setPageSize((int) pagee.getPageSize());
                pageVO.setList(collect);
                pageVO.setPage((int) pagee.getPage());

                //没有则查到后向redis中传一份
                String jsonStr = JSONUtil.toJsonStr(pageVO);

                stringRedisTemplate.opsForValue().set(RedisConstants.Post_Key+pagee.getPage(),jsonStr,30l+l, TimeUnit.MINUTES);

                //在这里释放锁
                //
                return pageVO;
            }finally {
                RedislockUtil.unlock(Post_lock+pagee.getPage());
            }

        }

        //redis中有直接返回就行了
        PageVO bean = JSONUtil.toBean(s, PageVO.class);
        return bean;
    }

    @Override
    public void deletepost(int id) {

        Post existing = this.getById(id);
        if (existing == null) {
            return;
        }
        this.removeById(id);

        // 读取端 key 为 RedisConstants.Post_Key + page，这里按前缀统一失效
        Set<String> keys = stringRedisTemplate.keys(RedisConstants.Post_Key + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }

    }

    private PageVO<PostVO> getPageVO(Pagee pagee) {

        //为防止长时间没有获得缓存值，这里只进行5次
        int count=65;
        while(count-->0){

            //自定义递归处理，防止卡在获取redis和获取锁之间，导致出现多线程问题
            String s = stringRedisTemplate.opsForValue().get(RedisConstants.Post_Key + pagee.getPage());
            if(s!=null){
                return JSONUtil.toBean(s, PageVO.class);
            }
            try{
                Thread.sleep(50);
            }catch(Exception e){
                break;
            }
        }
       return null;
    }

}
