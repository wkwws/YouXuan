package com.example.secondhand_project.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "文件上传返回信息")
public class FileVO {
    @Schema(description = "文件访问URL")
    private String url;

    @Schema(description = "文件名")
    private String filename;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "文件大小（字节）")
    private Integer size;

    @Schema(description = "文件MIME类型")
    private String mimeType;
}
