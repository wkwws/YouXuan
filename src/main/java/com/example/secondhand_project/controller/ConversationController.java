package com.example.secondhand_project.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 私信会话表 前端控制器
 * </p>
 *
 * @author wkw
 * @since 2026-06-02
 */
@RestController
@RequestMapping("/conversation")
@Tag(name = "09-会话管理", description = "私信会话管理（预留）")
public class ConversationController {

}
