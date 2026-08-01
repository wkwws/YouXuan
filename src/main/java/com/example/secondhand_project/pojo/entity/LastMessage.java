package com.example.secondhand_project.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LastMessage {

    private String content;

    private LocalDateTime createAt;

}
