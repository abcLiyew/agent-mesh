package com.esdllm.agentmesh.model.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String userRole;
    private Date createTime;

}
