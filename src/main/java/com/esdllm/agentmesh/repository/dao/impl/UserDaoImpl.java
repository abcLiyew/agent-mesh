package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.dto.request.UserRegisterRequest;
import com.esdllm.agentmesh.model.dto.response.UserResponse;
import com.esdllm.agentmesh.emun.UserRoleEnum;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.repository.dao.UserDao;
import com.esdllm.agentmesh.repository.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author LiYehe
* @description 针对表【user(用户信息表)】的数据库操作 Service 实现
* @createDate 2026-03-09 13:34:39
*/
@Service
public class UserDaoImpl extends ServiceImpl<UserMapper, User>
    implements UserDao {

    @Override
    public boolean isExistByUsername(String username) {
        User user = this.lambdaQuery()
            .eq(User::getUsername, username)
            .one();
        return user != null;
    }

    @Override
    public boolean isExistByEmail(String email) {
        User user = this.lambdaQuery()
            .eq(User::getEmail, email)
            .one();
        return user != null;
    }

    @Override
    public User getByUsername(String username) {
        return this.lambdaQuery()
            .eq(User::getUsername, username)
            .one();
    }

    @Override
    public Page<User> getActiveUsersPage(int page, int pageSize) {
        Page<User> userPage = new Page<>(page, pageSize);
        return this.lambdaQuery()
            .eq(User::getIsDelete, 0)
            .orderByDesc(User::getCreatedAt)
            .page(userPage);
    }

    @Override
    public User toUser(UserRegisterRequest userDO) {
        User user = new User();
        user.setUsername(userDO.getUsername());
        user.setPasswordHash(userDO.getPassword());
        user.setEmail(userDO.getEmail());

        return user;
    }

    @Override
    public UserResponse toUserRegisterResponse(User user) {
        if (user == null) {
            return null;
        }
        
        UserResponse userRegisterResponse = new UserResponse();
        userRegisterResponse.setId(user.getId());
        userRegisterResponse.setUsername(user.getUsername());
        userRegisterResponse.setEmail(user.getEmail());

        UserRoleEnum userRoleEnum = UserRoleEnum.of(user.getUserRole());
        userRegisterResponse.setUserRole(userRoleEnum.getDesc());

        userRegisterResponse.setCreateTime(user.getCreatedAt());

        return userRegisterResponse;
    }
}




