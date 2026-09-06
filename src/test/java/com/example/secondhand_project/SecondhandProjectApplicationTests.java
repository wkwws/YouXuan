package com.example.secondhand_project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecondhandProjectApplicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Test
    void contextLoads() {
    }

    @Test
    public void redistest(){

        stringRedisTemplate.opsForValue().setIfAbsent("lock","1",300, TimeUnit.SECONDS);

    }

}
