package com.esdllm.agentmesh.emun;

import lombok.Getter;

/**
 * 资源可见性级别
 */
@Getter
public enum ResourceVisibility {
    PRIVATE(0, "私有 - 仅创建者可见"),
    TEAM(1, "团队共享 - 团队成员可见"),
    PUBLIC(2, "公开 - 所有用户可见");
    
    private final int code;
    private final String desc;
    
    ResourceVisibility(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static ResourceVisibility of(int code) {
        for (ResourceVisibility item : values()) {
            if (item.code == code) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown visibility: " + code);
    }
}
