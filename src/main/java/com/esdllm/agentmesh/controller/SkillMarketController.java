package com.esdllm.agentmesh.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.model.domain.AgentSkillPackage;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.domain.UserSkillInstallation;
import com.esdllm.agentmesh.service.UserService;
import com.esdllm.agentmesh.service.unified.SkillMarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能市场控制器
 * 提供技能包的发布、搜索、安装、执行等功能
 */
@RestController
@RequestMapping("/api/skill-market")
@Tag(name = "技能市场", description = "技能包的发布、搜索、安装和管理")
@Slf4j
public class SkillMarketController {
    
    @Resource
    private SkillMarketService skillMarketService;
    
    @Resource
    private UserService userService;
    
    /**
     * 发布技能包
     */
    @PostMapping("/publish")
    @Operation(summary = "发布技能包", description = "发布新的技能包到技能市场")
    public BaseResponse<Map<String, Object>> publishSkill(
            @RequestBody AgentSkillPackage skillPackage,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("发布技能包请求，userId: {}, skillName: {}", loginUser.getId(), skillPackage.getSkillName());
        
        skillPackage.setAuthorId(loginUser.getId());
        Long skillId = skillMarketService.publishSkill(skillPackage);
        
        Map<String, Object> response = new HashMap<>();
        response.put("skillId", skillId);
        response.put("message", "技能包发布成功");
        
        return ResultUtils.success(response);
    }
    
    /**
     * 搜索技能包
     */
    @GetMapping("/search")
    @Operation(summary = "搜索技能包", description = "按关键词和分类搜索技能包")
    public BaseResponse<Page<AgentSkillPackage>> searchSkills(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "技能分类") @RequestParam(required = false) String category,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("搜索技能包，keyword: {}, category: {}, page: {}, pageSize: {}", 
                keyword, category, page, pageSize);
        
        List<AgentSkillPackage> skills = skillMarketService.searchSkills(
            keyword, category, page, pageSize
        );
        
        // 构建分页结果
        Page<AgentSkillPackage> resultPage = new Page<>(page, pageSize);
        resultPage.setRecords(skills);
        // TODO: 从服务层获取总数
        resultPage.setTotal(skills.size());
        
        return ResultUtils.success(resultPage);
    }
    
    /**
     * 获取技能详情
     */
    @GetMapping("/{skillId}")
    @Operation(summary = "获取技能详情", description = "根据ID获取技能包详细信息")
    public BaseResponse<AgentSkillPackage> getSkillDetail(
            @Parameter(description = "技能ID") @PathVariable Long skillId,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("获取技能详情，skillId: {}", skillId);
        
        AgentSkillPackage skill = skillMarketService.getSkillDetail(skillId);
        if (skill == null) {
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "技能包不存在");
        }
        
        return ResultUtils.success(skill);
    }
    
    /**
     * 安装技能
     */
    @PostMapping("/install")
    @Operation(summary = "安装技能", description = "为用户或智能体安装技能包")
    public BaseResponse<Map<String, Object>> installSkill(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        Long agentId = request.get("agentId") != null ? 
            Long.valueOf(request.get("agentId").toString()) : null;
        Long skillId = Long.valueOf(request.get("skillId").toString());
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) request.get("config");
        
        log.info("安装技能，userId: {}, agentId: {}, skillId: {}", 
                loginUser.getId(), agentId, skillId);
        
        Long installationId = skillMarketService.installSkill(
            loginUser.getId(), agentId, skillId, config
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("installationId", installationId);
        response.put("message", "技能安装成功");
        
        return ResultUtils.success(response);
    }
    
    /**
     * 卸载技能
     */
    @DeleteMapping("/uninstall")
    @Operation(summary = "卸载技能", description = "卸载已安装的技能")
    public BaseResponse<String> uninstallSkill(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        Long agentId = request.get("agentId") != null ? 
            Long.valueOf(request.get("agentId").toString()) : null;
        Long installationId = Long.valueOf(request.get("installationId").toString());
        
        log.info("卸载技能，userId: {}, agentId: {}, installationId: {}", 
                loginUser.getId(), agentId, installationId);
        
        skillMarketService.uninstallSkill(loginUser.getId(), agentId, installationId);
        
        return ResultUtils.success("卸载成功");
    }
    
    /**
     * 获取用户已安装技能
     */
    @GetMapping("/user-installed")
    @Operation(summary = "获取用户已安装技能", description = "获取当前用户安装的所有技能")
    public BaseResponse<List<Map<String, Object>>> getUserInstalledSkills(
            @Parameter(description = "智能体ID（可选）") @RequestParam(required = false) Long agentId,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("获取用户已安装技能，userId: {}, agentId: {}", loginUser.getId(), agentId);
        
        List<Map<String, Object>> installations = skillMarketService.getUserInstalledSkills(
            loginUser.getId(), agentId
        );
        
        return ResultUtils.success(installations);
    }
    
    /**
     * 启用/禁用技能
     */
    @PutMapping("/toggle")
    @Operation(summary = "启用/禁用技能", description = "切换技能的启用状态")
    public BaseResponse<String> toggleSkill(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        Long installationId = Long.valueOf(request.get("installationId").toString());
        Boolean enabled = Boolean.valueOf(request.get("enabled").toString());
        
        log.info("切换技能状态，userId: {}, installationId: {}, enabled: {}", 
                loginUser.getId(), installationId, enabled);
        
        skillMarketService.toggleSkill(installationId, enabled);
        
        return ResultUtils.success(enabled ? "已启用" : "已禁用");
    }
    
    /**
     * 执行技能
     */
    @PostMapping("/execute")
    @Operation(summary = "执行技能", description = "执行已安装的技能")
    public BaseResponse<Object> executeSkill(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        Long installationId = Long.valueOf(request.get("installationId").toString());
        @SuppressWarnings("unchecked")
        Map<String, Object> inputParams = (Map<String, Object>) request.get("inputParams");
        
        log.info("执行技能，userId: {}, installationId: {}", loginUser.getId(), installationId);
        
        Object result = skillMarketService.executeSkill(
            installationId, inputParams, loginUser.getId()
        );
        
        return ResultUtils.success(result);
    }
    
    /**
     * 评分技能
     */
    @PostMapping("/rate")
    @Operation(summary = "评分技能", description = "对技能进行评分和评论")
    public BaseResponse<String> rateSkill(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        Long skillId = Long.valueOf(request.get("skillId").toString());
        Double rating = Double.valueOf(request.get("rating").toString());
        String comment = request.get("comment") != null ? 
            request.get("comment").toString() : null;
        
        log.info("评分技能，userId: {}, skillId: {}, rating: {}", 
                loginUser.getId(), skillId, rating);
        
        skillMarketService.rateSkill(skillId, loginUser.getId(), rating, comment);
        
        return ResultUtils.success("评分成功");
    }
    
    /**
     * 获取热门技能
     */
    @GetMapping("/popular")
    @Operation(summary = "获取热门技能", description = "按下载量获取热门技能")
    public BaseResponse<List<AgentSkillPackage>> getPopularSkills(
            @Parameter(description = "技能分类（可选）") @RequestParam(required = false) String category,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") int limit,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("获取热门技能，category: {}, limit: {}", category, limit);
        
        List<AgentSkillPackage> skills = skillMarketService.getPopularSkills(category, limit);
        
        return ResultUtils.success(skills);
    }
    
    /**
     * 更新技能
     */
    @PutMapping("/{skillId}")
    @Operation(summary = "更新技能", description = "更新技能包信息")
    public BaseResponse<String> updateSkill(
            @PathVariable Long skillId,
            @RequestBody AgentSkillPackage updatedSkill,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("更新技能，skillId: {}, userId: {}", skillId, loginUser.getId());
        
        skillMarketService.updateSkill(skillId, updatedSkill);
        
        return ResultUtils.success("更新成功");
    }
    
    /**
     * 删除技能
     */
    @DeleteMapping("/{skillId}")
    @Operation(summary = "删除技能", description = "删除技能包（仅作者可删除）")
    public BaseResponse<String> deleteSkill(
            @PathVariable Long skillId,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("删除技能，skillId: {}, userId: {}", skillId, loginUser.getId());
        
        skillMarketService.deleteSkill(skillId, loginUser.getId());
        
        return ResultUtils.success("删除成功");
    }
}
