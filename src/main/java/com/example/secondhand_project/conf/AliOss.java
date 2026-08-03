package com.example.secondhand_project.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "secondhand.alioss")
public class AliOss {

    public static String endpoint;
    public static String accessKeyId;
    public static String accessKeySecret;
    public static String bucketName;

}
