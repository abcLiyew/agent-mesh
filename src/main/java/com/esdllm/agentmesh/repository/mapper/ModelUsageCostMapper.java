package com.esdllm.agentmesh.repository.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.esdllm.agentmesh.model.domain.ModelUsageCost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 模型使用成本 Mapper
 */
@Mapper
public interface ModelUsageCostMapper extends BaseMapper<ModelUsageCost> {

    /**
     * 查询用户成本统计
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 成本统计 Map
     */
    Map<String, Object> getUserCostStats(@Param("userId") Long userId, 
                                         @Param("startDate") Date startDate, 
                                         @Param("endDate") Date endDate);

    /**
     * 按模型分组统计成本
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 模型成本 Map 列表
     */
    List<Map<String, Object>> getCostByModel(@Param("userId") Long userId,
                                             @Param("startDate") Date startDate,
                                             @Param("endDate") Date endDate);

    /**
     * 按智能体分组统计调用次数
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 智能体调用统计 Map 列表
     */
    List<Map<String, Object>> getCallsByAgent(@Param("userId") Long userId,
                                              @Param("startDate") Date startDate,
                                              @Param("endDate") Date endDate);

    /**
     * 查询智能体成本统计
     * @param agentId 智能体 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 成本统计 Map
     */
    Map<String, Object> getAgentCostStats(@Param("agentId") Long agentId,
                                          @Param("startDate") Date startDate,
                                          @Param("endDate") Date endDate);

    /**
     * 查询成本趋势
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @return 成本趋势 Map 列表
     */
    List<Map<String, Object>> getCostTrend(@Param("userId") Long userId,
                                           @Param("startDate") Date startDate);
}
