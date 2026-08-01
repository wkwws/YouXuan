package com.example.secondhand_project.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "会话ID请求参数")
public class ConvId {
    @Schema(description = "会话ID", example = "1")
    private String convId;
}
