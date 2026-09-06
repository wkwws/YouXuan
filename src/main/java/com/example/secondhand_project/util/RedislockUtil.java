package com.example.secondhand_project.util;


import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

//redis分布式锁工具类

@Component
public class RedislockUtil {

    private static StringRedisTemplate stringRedisTemplate;
    //静态注入
    @Autowired
    public RedislockUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 获取分布锁
     * @param lockname
     * @return
     */
    public static boolean getlock(String lockname){
        //通过setnx来构建锁
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(lockname, "1",10L, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(b);

    }

    /**
     * 释放分布式锁
     * @param lockname
     */
    public static void unlock(String lockname){

        stringRedisTemplate.delete(lockname);

    }

}
