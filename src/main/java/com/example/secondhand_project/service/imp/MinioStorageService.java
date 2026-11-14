package com.example.secondhand_project.service.imp;

import com.example.secondhand_project.conf.MinioProperties;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.exception.BusinessException;
import com.example.secondhand_project.pojo.entity.Upload;
import com.example.secondhand_project.pojo.vo.FileVO;
import com.example.secondhand_project.service.IUploadService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * MinIO 图片存储：上传、公共读 bucket、上传记录落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final MinioClient minioClient;

    private final MinioProperties properties;

    private final IUploadService uploadService;

    private volatile boolean bucketReady = false;

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            ensureBucket();
        } catch (Exception e) {
            log.warn("MinIO 初始化失败，图片上传暂不可用: {}", e.getMessage());
        }
    }

    public FileVO uploadImage(MultipartFile file) {
        if (!properties.isEnabled()) {
            throw new BusinessException("对象存储未启用");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > properties.getMaxImageSize()) {
            throw new BusinessException("图片不能超过 " + (properties.getMaxImageSize() / 1024 / 1024) + "MB");
        }

        String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String extension = extensionOf(originalName);  //出去 文件类型的 原始文件名
        String mime = normalizeMime(file.getContentType(), extension);//排除
        if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_MIME_TYPES.contains(mime)) {
            throw new BusinessException("只支持 jpg、jpeg、png、webp、gif 图片");
        }

        String objectName = buildObjectName(extension);
        try {
            ensureBucket();
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(mime)
                        .build());
            }
        } catch (Exception e) {
            log.error("MinIO 图片上传失败, object={}", objectName, e);
            throw new BusinessException("图片上传失败，请稍后重试");
        }

        String url = buildPublicUrl(objectName);
        saveUploadRecord(objectName, originalName, url, mime, file.getSize());
        log.info("MinIO 图片上传成功, object={}, size={}", objectName, file.getSize());

        return FileVO.builder()
                .url(url)
                .filename(objectName)
                .originalName(originalName)
                .size((int) Math.min(file.getSize(), Integer.MAX_VALUE))
                .mimeType(mime)
                .build();
    }

    private synchronized void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        String bucket = properties.getBucket();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("MinIO bucket 已创建: {}", bucket);
        }
        if (properties.isPublicRead()) {
            try {
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(publicReadPolicy(bucket))
                        .build());
            } catch (Exception e) {
                // 权限不足但桶本身已经是公共读时，不应该阻塞上传
                log.warn("设置 MinIO 公共读策略失败，请确认 bucket 已允许匿名 GET: {}", e.getMessage());
            }
        }
        bucketReady = true;
    }

    private String publicReadPolicy(String bucket) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
    }

    private void saveUploadRecord(String objectName, String originalName, String url, String mime, long size) {
        try {
            Long currentId = BaseContext.getCurrentId();
            Upload record = new Upload()
                    .setFilename(truncate(objectName, 128))
                    .setOriginName(truncate(originalName, 256))
                    .setUrl(truncate(url, 512))
                    .setMime(truncate(mime, 50))
                    .setFsize((int) Math.min(size, Integer.MAX_VALUE))
                    .setUid(currentId == null ? 0 : currentId.intValue())
                    .setCreatedAt(LocalDateTime.now());
            uploadService.save(record);
        } catch (Exception e) {
            // 上传记录失败不影响图片本身
            log.warn("保存上传记录失败: {}", e.getMessage());
        }
    }

    private String buildObjectName(String extension) {
        return "images/" + LocalDate.now().format(DATE_PATH) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }

    private String buildPublicUrl(String objectName) {
        String base = properties.getPublicUrl();
        if (base == null || base.isBlank()) {
            base = properties.getEndpoint();
        }
        base = base == null ? "" : base.replaceAll("/+$", "");
        return base + "/" + properties.getBucket() + "/" + objectName;
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeMime(String contentType, String extension) {
        String mime = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!mime.startsWith("image/")) {
            mime = switch (extension) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "webp" -> "image/webp";
                case "gif" -> "image/gif";
                default -> mime;
            };
        }
        return "image/jpg".equals(mime) ? "image/jpeg" : mime;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
