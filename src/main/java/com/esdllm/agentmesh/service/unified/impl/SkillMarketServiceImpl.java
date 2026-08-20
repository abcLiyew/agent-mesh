package com.esdllm.agentmesh.service.unified.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.esdllm.agentmesh.model.domain.AgentSkillPackage;
import com.esdllm.agentmesh.model.domain.UserSkillInstallation;
import com.esdllm.agentmesh.repository.mapper.AgentSkillPackageMapper;
import com.esdllm.agentmesh.repository.mapper.UserSkillInstallationMapper;
import com.esdllm.agentmesh.service.unified.SkillMarketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 技能市场服务实现类（数据库持久化版本）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SkillMarketServiceImpl implements SkillMarketService {
    
    private final AgentSkillPackageMapper skillPackageMapper;
    private final UserSkillInstallationMapper installationMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    @Transactional
    public Long publishSkill(AgentSkillPackage skillPackage) {
        skillPackage.setStatus(1); // 1=发布
        skillPackage.setIsPublic(true);
        skillPackage.setDownloadCount(0);
        skillPackage.setRatingAvg(BigDecimal.ZERO);
        skillPackage.setRatingCount(0);
        skillPackage.setCreatedAt(new Date());
        skillPackage.setUpdatedAt(new Date());

        skillPackageMapper.insert(skillPackage);
        log.info("技能包发布成功，skillId: {}, name: {}", skillPackage.getId(), skillPackage.getSkillName());
        
        return skillPackage.getId();
    }
    
    @Override
    public List<AgentSkillPackage> searchSkills(String keyword, String category, 
                                               int page, int pageSize) {
        LambdaQueryWrapper<AgentSkillPackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentSkillPackage::getStatus, 1) // 只查询已发布的
               .eq(AgentSkillPackage::getIsDelete, 0)
               .orderByDesc(AgentSkillPackage::getRatingAvg);
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(AgentSkillPackage::getSkillName, keyword)
                             .or()
                             .like(AgentSkillPackage::getDescription, keyword));
        }
        
        if (category != null && !category.trim().isEmpty()) {
            wrapper.eq(AgentSkillPackage::getCategory, category);
        }
        
        int offset = (page - 1) * pageSize;
        wrapper.last("LIMIT " + pageSize + " OFFSET " + offset);
        
        return skillPackageMapper.selectList(wrapper);
    }
    
    @Override
    public AgentSkillPackage getSkillDetail(Long skillId) {
        return skillPackageMapper.selectById(skillId);
    }
    
    @Override
    @Transactional
    public Long installSkill(Long userId, Long agentId, Long skillId, Map<String, Object> config) {
        AgentSkillPackage skill = skillPackageMapper.selectById(skillId);
        if (skill == null) {
            throw new RuntimeException("技能包不存在");
        }
        
        // 检查是否已安装
        LambdaQueryWrapper<UserSkillInstallation> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(UserSkillInstallation::getUserId, userId)
                    .eq(UserSkillInstallation::getSkillId, skillId);
        UserSkillInstallation existing = installationMapper.selectOne(checkWrapper);
        
        if (existing != null) {
            log.warn("技能已安装，userId: {}, skillId: {}", userId, skillId);
            return existing.getId();
        }
        
        // 创建安装记录
        UserSkillInstallation installation = new UserSkillInstallation();
        installation.setUserId(userId);
        installation.setSkillId(skillId);
        installation.setInstallationConfigJson(config != null ? config.toString() : null);
        installation.setStatus(1); // 1=已安装
        installation.setInstalledAt(LocalDateTime.now());
        installation.setUpdatedAt(LocalDateTime.now());
        
        installationMapper.insert(installation);
        
        // 更新下载次数
        skill.setDownloadCount(skill.getDownloadCount() + 1);
        skillPackageMapper.updateById(skill);
        
        log.info("技能安装成功，installationId: {}, skillId: {}", installation.getId(), skillId);
        
        return installation.getId();
    }
    
    @Override
    @Transactional
    public void uninstallSkill(Long userId, Long agentId, Long installationId) {
        UserSkillInstallation installation = installationMapper.selectById(installationId);
        if (installation != null && installation.getUserId().equals(userId)) {
            installationMapper.deleteById(installationId);
            log.info("技能卸载成功，installationId: {}", installationId);
        } else {
            throw new RuntimeException("无权卸载该技能或安装记录不存在");
        }
    }
    
    @Override
    public List<Map<String, Object>> getUserInstalledSkills(Long userId, Long agentId) {
        LambdaQueryWrapper<UserSkillInstallation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkillInstallation::getUserId, userId);
        
        List<UserSkillInstallation> installations = installationMapper.selectList(wrapper);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserSkillInstallation inst : installations) {
            Map<String, Object> item = new HashMap<>();
            item.put("installationId", inst.getId());
            item.put("userId", inst.getUserId());
            item.put("skillId", inst.getSkillId());
            item.put("status", inst.getStatus());
            item.put("installedAt", inst.getInstalledAt());
            
            // 获取技能详情
            AgentSkillPackage skill = skillPackageMapper.selectById(inst.getSkillId());
            if (skill != null) {
                item.put("skillName", skill.getSkillName());
                item.put("skillCode", skill.getSkillCode());
                item.put("category", skill.getCategory());
            }
            
            result.add(item);
        }
        
        return result;
    }
    
    @Override
    @Transactional
    public void toggleSkill(Long installationId, Boolean enabled) {
        UserSkillInstallation installation = installationMapper.selectById(installationId);
        if (installation != null) {
            installation.setStatus(enabled ? 1 : 0); // 1=已安装, 0=已禁用
            installation.setUpdatedAt(LocalDateTime.now());
            installationMapper.updateById(installation);
            log.info("技能{}成功，installationId: {}", enabled ? "启用" : "禁用", installationId);
        } else {
            throw new RuntimeException("安装记录不存在");
        }
    }
    
    @Override
    @Transactional
    public Object executeSkill(Long installationId, Map<String, Object> inputParams, Long userId) {
        UserSkillInstallation installation = installationMapper.selectById(installationId);
        if (installation == null) {
            throw new RuntimeException("技能安装记录不存在");
        }
        
        if (!installation.getUserId().equals(userId)) {
            throw new RuntimeException("无权执行该技能");
        }
        
        if (installation.getStatus() != 1) {
            throw new RuntimeException("技能未启用");
        }
        
        Long skillId = installation.getSkillId();
        AgentSkillPackage skill = skillPackageMapper.selectById(skillId);
        
        if (skill == null) {
            throw new RuntimeException("技能包不存在");
        }
        
        log.info("执行技能，installationId: {}, skillName: {}", installationId, skill.getSkillName());
        
        try {
            // 解析技能配置
            Map<String, Object> config = parseSkillConfig(skill.getSkillConfigJson());
            
            if (config == null || config.isEmpty()) {
                log.warn("技能配置为空，返回演示结果");
                return Map.of(
                    "success", true,
                    "message", "技能执行成功（无配置）",
                    "skillId", skillId,
                    "skillName", skill.getSkillName()
                );
            }
            
            // 根据配置类型执行不同的逻辑
            String executionType = (String) config.getOrDefault("executionType", "simple");
            
            Map<String, Object> result;
            switch (executionType) {
                case "workflow":
                    // 工作流类型：执行预定义的工作流步骤
                    result = executeWorkflowSkill(config, inputParams);
                    break;
                    
                case "tool_chain":
                    // 工具链类型：按顺序调用多个工具
                    result = executeToolChainSkill(config, inputParams);
                    break;
                    
                case "template":
                    // 模板类型：使用模板生成内容
                    result = executeTemplateSkill(config, inputParams);
                    break;
                    
                default:
                    // 简单类型：直接返回配置中的示例结果
                    result = executeSimpleSkill(config, inputParams);
            }
            
            // 添加技能元数据
            result = new HashMap<>(result);
            result.put("skillId", skillId);
            result.put("skillName", skill.getSkillName());
            result.put("executionType", executionType);
            
            log.info("技能执行成功: {}", skill.getSkillName());
            return result;
            
        } catch (Exception e) {
            log.error("技能执行失败", e);
            return Map.of(
                "success", false,
                "error", "技能执行失败: " + e.getMessage(),
                "skillId", skillId
            );
        }
    }
    
    /**
     * 解析技能配置JSON
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSkillConfig(String configJson) {
        if (StrUtil.isBlank(configJson)) {
            return Collections.emptyMap();
        }
        
        try {
            return objectMapper.readValue(configJson, Map.class);
        } catch (Exception e) {
            log.error("技能配置JSON解析失败", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * 执行工作流类型技能
     */
    private Map<String, Object> executeWorkflowSkill(Map<String, Object> config, Map<String, Object> parameters) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) config.getOrDefault("steps", Collections.emptyList());
        
        log.info("执行工作流技能，步骤数: {}", steps.size());
        
        // 简化实现：返回步骤列表
        return Map.of(
            "success", true,
            "message", "工作流技能执行完成",
            "totalSteps", steps.size(),
            "executedSteps", steps.size()
        );
    }
    
    /**
     * 执行工具链类型技能
     */
    private Map<String, Object> executeToolChainSkill(Map<String, Object> config, Map<String, Object> parameters) {
        @SuppressWarnings("unchecked")
        List<Long> toolIds = (List<Long>) config.getOrDefault("toolIds", Collections.emptyList());
        
        log.info("执行工具链技能，工具数: {}", toolIds.size());
        
        // 简化实现：返回工具列表
        return Map.of(
            "success", true,
            "message", "工具链技能执行完成",
            "toolCount", toolIds.size(),
            "tools", toolIds
        );
    }
    
    /**
     * 执行模板类型技能
     */
    private Map<String, Object> executeTemplateSkill(Map<String, Object> config, Map<String, Object> parameters) {
        String template = (String) config.getOrDefault("template", "");
        
        log.info("执行模板技能，模板长度: {}", template.length());
        
        // 简化实现：替换模板变量
        String result = template;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        
        return Map.of(
            "success", true,
            "message", "模板技能执行完成",
            "output", result
        );
    }
    
    /**
     * 执行简单类型技能
     */
    private Map<String, Object> executeSimpleSkill(Map<String, Object> config, Map<String, Object> parameters) {
        Object exampleOutput = config.get("exampleOutput");
        
        return Map.of(
            "success", true,
            "message", "简单技能执行完成",
            "output", exampleOutput != null ? exampleOutput : "执行成功"
        );
    }
    
    @Override
    @Transactional
    public void rateSkill(Long skillId, Long userId, Double rating, String comment) {
        AgentSkillPackage skill = skillPackageMapper.selectById(skillId);
        if (skill == null) {
            throw new RuntimeException("技能包不存在");
        }
        
        // 简化处理：直接更新平均评分
        // 实际应该维护一个评分表，计算平均值
        BigDecimal newRating = BigDecimal.valueOf(rating);
        int currentCount = skill.getRatingCount() != null ? skill.getRatingCount() : 0;
        BigDecimal currentAvg = skill.getRatingAvg() != null ? skill.getRatingAvg() : BigDecimal.ZERO;
        
        // 计算新的平均值（简化算法）
        BigDecimal newAvg = currentAvg.multiply(BigDecimal.valueOf(currentCount))
                                     .add(newRating)
                                     .divide(BigDecimal.valueOf(currentCount + 1), 2, BigDecimal.ROUND_HALF_UP);
        
        skill.setRatingAvg(newAvg);
        skill.setRatingCount(currentCount + 1);
        skill.setUpdatedAt(new Date());
        skillPackageMapper.updateById(skill);
        
        log.info("技能评分成功，skillId: {}, rating: {}, avgRating: {}", skillId, rating, newAvg);
    }
    
    @Override
    public List<AgentSkillPackage> getPopularSkills(String category, int limit) {
        LambdaQueryWrapper<AgentSkillPackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentSkillPackage::getStatus, 1)
               .eq(AgentSkillPackage::getIsDelete, 0)
               .orderByDesc(AgentSkillPackage::getDownloadCount)
               .last("LIMIT " + limit);
        
        if (category != null && !category.trim().isEmpty()) {
            wrapper.eq(AgentSkillPackage::getCategory, category);
        }
        
        return skillPackageMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional
    public void updateSkill(Long skillId, AgentSkillPackage updatedSkill) {
        AgentSkillPackage existing = skillPackageMapper.selectById(skillId);
        if (existing == null) {
            throw new RuntimeException("技能包不存在");
        }
        
        updatedSkill.setId(skillId);
        updatedSkill.setDownloadCount(existing.getDownloadCount());
        updatedSkill.setRatingAvg(existing.getRatingAvg());
        updatedSkill.setRatingCount(existing.getRatingCount());
        updatedSkill.setCreatedAt(existing.getCreatedAt());
        updatedSkill.setUpdatedAt(new Date());
        updatedSkill.setIsDelete(existing.getIsDelete());
        
        skillPackageMapper.updateById(updatedSkill);
        log.info("技能包更新成功，skillId: {}", skillId);
    }
    
    @Override
    @Transactional
    public void deleteSkill(Long skillId, Long userId) {
        AgentSkillPackage skill = skillPackageMapper.selectById(skillId);
        if (skill == null) {
            throw new RuntimeException("技能包不存在");
        }
        
        if (!userId.equals(skill.getAuthorId())) {
            throw new RuntimeException("无权删除该技能包");
        }
        
        // 逻辑删除
        skill.setIsDelete(1);
        skill.setUpdatedAt(new Date());
        skillPackageMapper.updateById(skill);
        
        log.info("技能包删除成功，skillId: {}", skillId);
    }
}
