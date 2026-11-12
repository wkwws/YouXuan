package com.example.secondhand_project.admin.dto;

import lombok.Data;

@Data
public class AdminUserUpdateDTO {

    private String nickname;

    private String email;

    private String phone;

    private String bio;

    private String avatar;

    private Integer role;
}
