package com.esdllm.agentmesh.repository.dao;

import com.esdllm.agentmesh.model.dto.request.UserRegisterRequest;
import com.esdllm.agentmesh.model.dto.response.UserResponse;
import com.esdllm.agentmesh.model.domain.User;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Repository;

/**
* @author LiYehe
* @description 针对表【user(用户信息表)】的数据库操作 Service
* @createDate 2026-03-09 13:26:59
*/
@Repository
public interface UserDao extends IService<User> {
    /**
     * 用户名是否存在
     */
    boolean isExistByUsername(String username);

    boolean isExistByEmail(String email);
    
    /**
     * 根据用户名查询用户
     */
    User getByUsername(String username);
    
    /**
     * 分页获取活跃用户列表（排除已删除）
     */
    Page<User> getActiveUsersPage(int page, int pageSize);
    
    /**
     * 类型转换
     */
    User toUser(UserRegisterRequest userDO);
    UserResponse toUserRegisterResponse(User user);
}
