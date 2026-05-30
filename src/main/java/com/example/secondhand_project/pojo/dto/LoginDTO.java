package com.example.secondhand_project.pojo.dto;


import lombok.Data;

@Data
public class LoginDTO {
    private String username;
    private String password;

    private String email;
    private String nickname;
}
