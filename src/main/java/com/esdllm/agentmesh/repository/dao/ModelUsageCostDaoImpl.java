package com.esdllm.agentmesh.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.ModelUsageCost;
import com.esdllm.agentmesh.repository.dao.ModelUsageCostDao;
import com.esdllm.agentmesh.repository.mapper.ModelUsageCostMapper;
import org.springframework.stereotype.Service;

/**
* @author LiYehe
* @description 针对表【model_usage_cost(模型调用成本记录)】的数据库操作 Service 实现
* @createDate 2026-03-10
*/
@Service
public class ModelUsageCostDaoImpl extends ServiceImpl<ModelUsageCostMapper, ModelUsageCost>
    implements ModelUsageCostDao {

}
