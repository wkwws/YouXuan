package com.example.secondhand_project.controller;

import com.example.secondhand_project.annotation.RateLimit;
import com.example.secondhand_project.pojo.vo.FileVO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.imp.MinioStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
@Tag(name = "08-文件管理", description = "图片上传（MinIO）")
public class FileController {

    private final MinioStorageService storageService;

    public FileController(MinioStorageService storageService) {
        this.storageService = storageService;
    }

    @Operation(summary = "上传图片", description = "上传图片到 MinIO，返回可直接展示的 URL（需登录）")
    @RateLimit(count = 10, window = 60, key = "upload:image",
            message = "上传过于频繁，请稍后再试")
    @PostMapping
    public Result<FileVO> upload(
            @Parameter(description = "上传的图片") @RequestParam("file") MultipartFile file) {
        return Result.success(storageService.uploadImage(file));
    }
}
