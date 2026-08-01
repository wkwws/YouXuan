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

        //构造要删除的key值
        double count = this.query().count();
        count=Math.ceil(count/15);
        int  countt = (int) count;
        List<String> keys=new ArrayList<>();
        for(int i=1;i<=countt;i++){
            keys.add(Post_Key+":"+i);
        }
        //删除redis中post的所有缓存数据
        stringRedisTemplate.delete(keys);

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
         *
         *
         */

        //设计随机过期时间
        long l = RandomUtil.randomLong(30);

        pagee.setPage(pagee.getPage()+1);
        String s = stringRedisTemplate.opsForValue().get(RedisConstants.Post_Key + pagee.getPage());

        if(s==null){
            //利用分布式锁来解决多线程问题
            boolean getlock = RedislockUtil.getlock(Post_lock+pagee.getPage());
            if(!getlock){
                PageVO<PostVO> pageVO = getPageVO(pagee);
                //如果获取锁失败了，就返回递归
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

        //先删除数据库中的内容
        this.removeById(id);
        //然后根据id值查到他所在的页数
        Post id1 = this.query().eq("id", id).one();
        LocalDateTime createdAt = id1.getCreatedAt();
        Long createdAt1 = this.query().gt("createdAt", createdAt).count();
        Long count = this.query().count();
        double countt=count;
        //总共多少页
        countt=Math.ceil(countt/15);
        //需要删除的页数
        double createdAt2=createdAt1;
        createdAt2=Math.ceil(createdAt2/15);

        List<String> keys=new ArrayList<>();
        for(int i=(int)(countt-createdAt2);i<=countt;i++){
            keys.add(Post_Key+":"+i);
        }
        stringRedisTemplate.delete(keys);

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
