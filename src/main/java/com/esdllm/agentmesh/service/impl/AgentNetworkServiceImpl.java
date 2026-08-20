package com.esdllm.agentmesh.service.impl;

import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.model.domain.AgentDependencyEntity;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.repository.dao.AgentDependencyDao;
import com.esdllm.agentmesh.service.AgentDependency;
import com.esdllm.agentmesh.service.AgentNetworkService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体网络服务实现类
 * 管理智能体之间的依赖和协作关系
 */
@Service
@Slf4j
public class AgentNetworkServiceImpl implements AgentNetworkService {

    @Resource
  private AgentDao agentDao;

    @Resource
  private AgentDependencyDao agentDependencyDao;

    // ... existing code ...

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAgentDependency(Long agentId, Long dependsOnAgentId, String dependencyType, Integer priority, Long userId) {
        AgentDependency dependency = new AgentDependency();
        dependency.setAgentId(agentId);
        dependency.setDependsOnAgentId(dependsOnAgentId);
        dependency.setDependencyType(dependencyType != null ? dependencyType : "CALL");
        dependency.setPriority(priority != null ? priority : 0);
        addAgentDependency(dependency, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAgentDependency(AgentDependency dependency, Long userId) {
      log.info("添加智能体依赖，agentId: {}, dependsOn: {}",
                dependency.getAgentId(), dependency.getDependsOnAgentId());

        // 1. 验证智能体存在性和权限
       Agent agent= agentDao.getById(dependency.getAgentId());
       Agent targetAgent= agentDao.getById(dependency.getDependsOnAgentId());

        if (agent == null || targetAgent == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在");
        }

        if (!agent.getUserId().equals(userId) || !targetAgent.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限操作该智能体");
        }

        // 2. 检查依赖关系是否已存在
        AgentDependencyEntity existing = agentDependencyDao.getByPair(
            dependency.getAgentId(), dependency.getDependsOnAgentId()
        );
        
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该依赖关系已存在");
        }

        // 3. 构建实体并保存
        AgentDependencyEntity entity = new AgentDependencyEntity();
        entity.setAgentId(dependency.getAgentId());
        entity.setDependsOnAgentId(dependency.getDependsOnAgentId());
        entity.setDependencyType(dependency.getDependencyType());
        entity.setPriority(dependency.getPriority());
        entity.setCreatedBy(userId);
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());

        boolean success = agentDependencyDao.save(entity);
        if (!success) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存依赖关系失败");
        }

        // 4. 检测循环依赖
        if (hasCircularDependency(dependency.getAgentId())) {
            // 回滚：删除刚保存的记录
            agentDependencyDao.delete(entity.getId());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "检测到循环依赖");
        }

      log.info("智能体依赖添加成功，id: {}", entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAgentDependency(Long agentId, Long dependsOnAgentId, Long userId) {
      log.info("移除智能体依赖，agentId: {}, dependsOn: {}", agentId, dependsOnAgentId);

        // 验证权限
        Agent agent = agentDao.getById(agentId);
        if (agent != null && !agent.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限操作该智能体");
        }

        // 查询依赖关系
        AgentDependencyEntity entity = agentDependencyDao.getByPair(agentId, dependsOnAgentId);
        if (entity == null) {
            log.warn("依赖关系不存在，agentId: {}, dependsOn: {}", agentId, dependsOnAgentId);
            return;
        }

        // 删除依赖关系
        boolean success = agentDependencyDao.delete(entity.getId());
        if (success) {
          log.info("智能体依赖移除成功，id: {}", entity.getId());
        } else {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除依赖关系失败");
        }
    }

    @Override
    public List<AgentDependency> getAgentDependencies(Long agentId) {
        if (agentId == null) {
            return new ArrayList<>();
        }

        List<AgentDependencyEntity> entities = agentDependencyDao.listByAgentId(agentId);
        if (entities.isEmpty()) {
            return new ArrayList<>();
        }

        return entities.stream()
                      .map(e -> new AgentDependency(
                          e.getAgentId(), 
                          e.getDependsOnAgentId(), 
                          e.getDependencyType(),
                          e.getPriority()
                      ))
                      .toList();
    }

    @Override
    public boolean hasCircularDependency(Long agentId) {
        if (agentId == null) {
            return false;
        }

        Set<Long> visited = new HashSet<>();
        return dfsDetectCycle(agentId, visited);
    }

    @Override
    public Object getNetworkTopology(Long userId) {
      log.info("生成智能体网络拓扑图，userId: {}", userId);

        // 构建拓扑图数据
       Map<String, Object> topology = new HashMap<>();
       List<Map<String, Object>> nodes = new ArrayList<>();
       List<Map<String, Object>> edges = new ArrayList<>();

        // 获取用户的所有智能体
       List<Agent> agents= getUserAgents(userId);

        for (Agent agent : agents) {
           Map<String, Object> node = new HashMap<>();
            node.put("id", agent.getId());
            node.put("name", agent.getName());
            node.put("type", "agent");
            nodes.add(node);

            // 从数据库查询依赖关系
            List<AgentDependencyEntity> dependencies = agentDependencyDao.listByAgentId(agent.getId());
            for (AgentDependencyEntity dep : dependencies) {
               Map<String, Object> edge = new HashMap<>();
                edge.put("source", agent.getId());
                edge.put("target", dep.getDependsOnAgentId());
                edge.put("type", dep.getDependencyType());
                edge.put("priority", dep.getPriority());
                edges.add(edge);
            }
        }

       topology.put("nodes", nodes);
       topology.put("edges", edges);

        return topology;
    }

    /**
     * DFS 检测循环依赖
     */
  private boolean dfsDetectCycle(Long current, Set<Long> visited) {
        if (visited.contains(current)) {
            return true;
        }

        visited.add(current);

        // 从数据库查询当前智能体的依赖
        List<AgentDependencyEntity> dependencies = agentDependencyDao.listByAgentId(current);
        for (AgentDependencyEntity dep : dependencies) {
            if (dfsDetectCycle(dep.getDependsOnAgentId(), visited)) {
                return true;
            }
        }

        visited.remove(current);
        return false;
    }

    /**
     * 获取用户的所有智能体
     */
  private List<Agent> getUserAgents(Long userId) {
        return agentDao.getUserAgents(userId);
    }
}
