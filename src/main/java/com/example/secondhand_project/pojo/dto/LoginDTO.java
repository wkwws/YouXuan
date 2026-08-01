package com.example.secondhand_project.pojo.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录/注册请求参数")
public class LoginDTO {
    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "邮箱（注册时选填）", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "昵称（注册时选填）", example = "张三")
    private String nickname;
}
