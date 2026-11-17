package com.example.secondhand_project.enums;

/**
 * 限流维度。
 *
 * <p>决定限流计数按什么身份区分：登录用户、客户端 IP，或两者自动选择。</p>
 */
public enum RateLimitDimension {

    /** 仅按登录用户限流；拿不到用户时放行 */
    USER,

    /** 仅按客户端 IP 限流 */
    IP,

    /** 有登录用户按用户，否则按 IP */
    USER_OR_IP
}
