package com.esdllm.agentmesh.model.dto.request;


import lombok.Data;

@Data
public class UserRegisterRequest {
    private String username;
    private String password;
    private String email;
    private String checkPassword;
}
