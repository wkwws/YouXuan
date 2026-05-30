package com.example.secondhand_project;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("com.example.secondhand_project.mapper")
public class SecondhandProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecondhandProjectApplication.class, args);
    }

}
