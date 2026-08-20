package com.esdllm.agentmesh.service.unified;

import com.esdllm.agentmesh.model.domain.AgentSkillPackage;

import java.util.List;
import java.util.Map;

/**
 * 技能市场服务接口
 * 实现"龙虾"的技能市场机制，支持能力包的发现、安装和管理
 */
public interface SkillMarketService {
    
    /**
     * 发布技能包
     * @param skillPackage 技能包
     * @return 技能包ID
     */
    Long publishSkill(AgentSkillPackage skillPackage);
    
    /**
     * 搜索技能包
     * @param keyword 关键词
     * @param category 分类（可选）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 技能包列表
     */
    List<AgentSkillPackage> searchSkills(String keyword, String category, 
                                        int page, int pageSize);
    
    /**
     * 获取技能包详情
     * @param skillId 技能包ID
     * @return 技能包
     */
    AgentSkillPackage getSkillDetail(Long skillId);
    
    /**
     * 安装技能包
     * @param userId 用户ID
     * @param agentId 智能体ID（可选）
     * @param skillId 技能包ID
     * @param config 安装配置
     * @return 安装记录ID
     */
    Long installSkill(Long userId, Long agentId, Long skillId, Map<String, Object> config);
    
    /**
     * 卸载技能包
     * @param userId 用户ID
     * @param agentId 智能体ID（可选）
     * @param installationId 安装记录ID
     */
    void uninstallSkill(Long userId, Long agentId, Long installationId);
    
    /**
     * 获取用户已安装的技能列表
     * @param userId 用户ID
     * @param agentId 智能体ID（可选）
     * @return 安装记录列表
     */
    List<Map<String, Object>> getUserInstalledSkills(Long userId, Long agentId);
    
    /**
     * 启用/禁用技能
     * @param installationId 安装记录ID
     * @param enabled 是否启用
     */
    void toggleSkill(Long installationId, Boolean enabled);
    
    /**
     * 执行技能
     * @param installationId 安装记录ID
     * @param inputParams 输入参数
     * @param userId 用户ID
     * @return 执行结果
     */
    Object executeSkill(Long installationId, Map<String, Object> inputParams, Long userId);
    
    /**
     * 评价技能
     * @param skillId 技能包ID
     * @param userId 用户ID
     * @param rating 评分（1-5）
     * @param comment 评论（可选）
     */
    void rateSkill(Long skillId, Long userId, Double rating, String comment);
    
    /**
     * 获取热门技能
     * @param category 分类（可选）
     * @param limit 返回数量
     * @return 技能包列表
     */
    List<AgentSkillPackage> getPopularSkills(String category, int limit);
    
    /**
     * 更新技能包
     * @param skillId 技能包ID
     * @param updatedSkill 更新的技能包信息
     */
    void updateSkill(Long skillId, AgentSkillPackage updatedSkill);
    
    /**
     * 删除技能包
     * @param skillId 技能包ID
     * @param userId 用户ID（必须是提供者）
     */
    void deleteSkill(Long skillId, Long userId);
}
