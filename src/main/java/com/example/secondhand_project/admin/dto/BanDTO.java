package com.example.secondhand_project.admin.dto;

import lombok.Data;

@Data
public class BanDTO {

    private String reason;

    /** 0 或空表示永久 */
    private Integer days;
}
