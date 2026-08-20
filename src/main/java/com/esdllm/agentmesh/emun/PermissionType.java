package com.esdllm.agentmesh.emun;

import lombok.Getter;

/**
 * 权限类型枚举
 */
@Getter
public enum PermissionType {
    READ(1, "读取"),
    WRITE(2, "写入"),
    DELETE(3, "删除"),
    ADMIN(4, "管理");
    
    private final int code;
    private final String desc;
    
    PermissionType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static PermissionType of(int code) {
        for (PermissionType item : values()) {
            if (item.code == code) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown permission type: " + code);
    }
}
