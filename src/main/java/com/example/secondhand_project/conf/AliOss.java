package com.example.secondhand_project.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "secondhand.alioss")
public class AliOss {

//    注意 配置类 不可以 给 static 静态变量赋值

    public  String endpoint;
    public  String accessKeyId;
    public  String accessKeySecret;
    public  String bucketName;

}
