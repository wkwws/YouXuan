package com.example.secondhand_project.pojo.vo;

import com.example.secondhand_project.pojo.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录成功返回信息")
public class LoginVO {

    @Schema(description = "JWT Token（后续请求需在 Header 中携带 Authorization: Bearer <token>）")
    private String Token;

    @Schema(description = "当前登录用户信息")
    private User user;
}
