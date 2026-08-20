package com.esdllm.agentmesh.repository.dao.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.UserCostThreshold;
import com.esdllm.agentmesh.repository.dao.UserCostThresholdDao;
import com.esdllm.agentmesh.repository.mapper.UserCostThresholdMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户成本阈值配置 DAO
 */
@Repository
public class UserCostThresholdDaoImpl extends ServiceImpl<UserCostThresholdMapper, UserCostThreshold>
        implements UserCostThresholdDao {
}
