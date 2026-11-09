package com.example.secondhand_project.util; // 工具类所在的包


import lombok.Data; // 旧代码保留的导入
import org.springframework.beans.factory.annotation.Autowired; // 构造器注入
import org.springframework.data.redis.core.RedisTemplate; // 旧代码保留的导入
import org.springframework.data.redis.core.StringRedisTemplate; // 操作 Redis 字符串
import org.springframework.stereotype.Component; // 交给 Spring 管理

import java.util.concurrent.TimeUnit; // 时间单位

//redis分布式锁工具类

@Component // 注册成 Spring Bean，让静态方法也能拿到 RedisTemplate
public class RedislockUtil {

    private static StringRedisTemplate stringRedisTemplate; // 静态字段：让 static 方法也能用

    //静态注入
    @Autowired // Spring 创建这个 Bean 时，把 StringRedisTemplate 传进来
    public RedislockUtil(StringRedisTemplate stringRedisTemplate) { // 构造器
        this.stringRedisTemplate = stringRedisTemplate; // 赋给静态字段
    }

    /**
     * 获取分布锁（默认 10 秒）
     * @param lockname 锁的名字
     * @return 是否抢到锁
     */
    public static boolean getlock(String lockname){ // 默认 10 秒版本
        return getlock(lockname, 10L, TimeUnit.SECONDS); // 复用下面的重载方法
    }

    /**
     * 获取分布锁，可指定超时时间。
     *
     * <p>重建随机页这种可能超过 10 秒的任务，应该传更长的 TTL，
     * 避免锁提前过期后两个线程同时写临时 key。</p>
     */
    public static boolean getlock(String lockname, long timeout, TimeUnit unit){ // 可指定超时时间的版本
        //通过setnx来构建锁
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(lockname, "1", timeout, unit); // SET key 1 NX EX timeout：没人占才成功
        return Boolean.TRUE.equals(b); // true 表示抢到锁，false 表示别人已经占着
    }

    /**
     * 释放分布式锁
     * @param lockname 锁的名字
     */
    public static void unlock(String lockname){ // 释放锁

        stringRedisTemplate.delete(lockname); // 直接删除锁 key

    }

}
