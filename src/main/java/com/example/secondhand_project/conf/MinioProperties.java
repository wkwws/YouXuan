package com.example.secondhand_project.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 对象存储配置。
 *
 * <p>endpoint 是后端访问 MinIO 的地址；publicUrl 是浏览器访问图片的地址。
 * 本地 Docker 两者通常都是 http://localhost:9000；部署后可以是 Nginx 反代地址。</p>
 */
@Data
@ConfigurationProperties(prefix = "secondhand.minio")
public class MinioProperties {

    private boolean enabled = true;

    /** 后端访问 MinIO 的地址，例如 http://localhost:9000 */
    private String endpoint = "http://localhost:9000";

    private String accessKey = "minioadmin";

    private String secretKey = "minioadmin";

    /** 存放图片的桶名 */
    private String bucket = "liuzhuanpin";

    /** 浏览器访问图片的地址；为空时使用 endpoint */
    private String publicUrl = "http://localhost:9000";

    /** 单张图片最大字节数，默认 10MB */
    private long maxImageSize = 10 * 1024 * 1024;

    /** 是否把桶设置为公共读（图片要能直接在 img 标签里展示） */
    private boolean publicRead = true;
}
