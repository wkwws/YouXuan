package com.example.secondhand_project.conf;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "secondhand.jwt")
public class JwtConf {
    //jwt相关配置
    private  String secretKey;
    private  long ttlMills;
    private  String userTokenName;
}
