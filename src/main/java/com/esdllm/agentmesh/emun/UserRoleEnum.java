package com.esdllm.agentmesh.emun;

import lombok.Getter;

@Getter
public enum UserRoleEnum {
    USER(0, "正式会员"),
    VIP(1, "大会员"),
    ADMIN(90, "管理员"),
    SUPER_ADMIN(99, "超级管理员");

    private final int code;
    private final String desc;

    UserRoleEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 静态方法：根据 int 获取枚举
    public static UserRoleEnum of(int code) {
        for (UserRoleEnum item : values()) {
            if (item.code == code) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown user role: " + code);
    }

    // 权限判断示例
    public boolean isAdminOrHigher() {
        return this.code >= 10;
    }

    public boolean isSuperAdmin() {
        return this.code == 99;
    }
}