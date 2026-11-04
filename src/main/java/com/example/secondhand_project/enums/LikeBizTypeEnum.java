package com.example.secondhand_project.enums;

import lombok.Getter;

/**
 * 点赞业务类型枚举。
 *
 * <p>
 * 枚举名（POST / COMMENT）就是 Redis key、MQ routing key、前端 bizType 字段使用的值；
 * code 预留给数据库 like_record.target_type 等需要数字的地方。
 * </p>
 */
@Getter
public enum LikeBizTypeEnum {

    POST(1, "说说"),
    COMMENT(2, "评论");

    private final Integer code;

    private final String desc;

    LikeBizTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据业务类型名获取枚举，大小写不敏感。
     */
    public static LikeBizTypeEnum fromName(String name) {
        if (name == null) {
            return null;
        }
        for (LikeBizTypeEnum value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据数字 code 获取枚举，预留给数据库字段。
     */
    public static LikeBizTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (LikeBizTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

}
