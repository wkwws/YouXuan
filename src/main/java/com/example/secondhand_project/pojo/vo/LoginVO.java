package com.example.secondhand_project.pojo.vo;

import com.example.secondhand_project.pojo.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {

    private String Token;

    private User user;
}
