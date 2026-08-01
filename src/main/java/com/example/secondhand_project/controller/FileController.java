package com.example.secondhand_project.controller;


import cn.hutool.json.JSONUtil;
import com.example.secondhand_project.pojo.vo.FileVO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.util.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/upload")
@Tag(name = "08-文件管理", description = "文件上传（阿里云 OSS）")
public class FileController {


    /**
     * 阿里文件上传
     * @param file
     * @return
     * @throws Exception
     */
    @Operation(summary = "上传文件", description = "上传文件到阿里云 OSS，返回文件 URL（需登录）")
    @PostMapping()
    public Result Controller_upload(
            @Parameter(description = "上传的文件") MultipartFile file) throws Exception {
        //通过Multipartfile 的getOriginalFilename方法 生成名字
        String originalFilename = file.getOriginalFilename();
        //通过uuid 方法将originalfilename进行加密
        String newFilename= UUID.randomUUID().toString()+"."+originalFilename.substring(originalFilename.lastIndexOf("."));
//        //通过transferTo将其传输到本地磁盘中
//        file.transferTo(new File("D:\\files\\"+newFilename));
        String upload = AliOssUtil.upload(newFilename, file.getInputStream());
        FileVO build = FileVO.builder().url(upload).filename(newFilename).build();
        String jsonStr = JSONUtil.toJsonStr(build);
        return Result.success(build);
    }
}
