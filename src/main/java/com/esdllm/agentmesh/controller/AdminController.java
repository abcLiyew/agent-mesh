package com.esdllm.agentmesh.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.emun.UserRoleEnum;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.domain.KnowledgeBase;
import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;
import com.esdllm.agentmesh.model.domain.McpServers;
import com.esdllm.agentmesh.model.domain.ModelProvider;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.AdminPageResult;
import com.esdllm.agentmesh.model.dto.response.AgentResponse;
import com.esdllm.agentmesh.service.AgentService;
import com.esdllm.agentmesh.service.AiModelService;
import com.esdllm.agentmesh.service.DashboardService;
import com.esdllm.agentmesh.service.KnowledgeBaseService;
import com.esdllm.agentmesh.service.McpServerService;
import com.esdllm.agentmesh.service.ModelProviderService;
import com.esdllm.agentmesh.service.ToolService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统管理后台统一接口
 */
@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {

    @Resource
    private UserService userService;

    @Resource
    private AgentService agentService;
    
    @Resource
    private ToolService toolService;
    
    @Resource
    private KnowledgeBaseService knowledgeBaseService;
    
    @Resource
    private AiModelService aiModelService;
    
    @Resource
    private McpServerService mcpServerService;
    
    @Resource
    private ModelProviderService modelProviderService;
    
    @Resource
    private com.esdllm.agentmesh.service.SysDictService sysDictService;

    /**
     * 检查管理员权限
     */
    private void checkAdminPermission(HttpSession session) {
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        UserRoleEnum role = UserRoleEnum.of(loginUser.getUserRole());
        if (!role.isAdminOrHigher()) {
            throw new BusinessException(ErrorCode.NO_AUTH, "需要管理员权限");
        }
    }

    // ==================== 用户管理 ====================

    /**
     * 获取用户分页列表
     */
    @GetMapping("/users")
    public BaseResponse<AdminPageResult<User>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        Page<User> userPage = userService.getUsersPage(page, pageSize);
        
        AdminPageResult<User> result = AdminPageResult.<User>builder()
                .list(userPage.getRecords())
                .total(userPage.getTotal())
                .page(page)
                .pageSize(pageSize)
                .build();
        
        return ResultUtils.success(result);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{userId}")
    public BaseResponse<Boolean> deleteUser(
            @PathVariable Long userId,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        Boolean result = userService.deleteUser(userId);
        return ResultUtils.success(result);
    }

    // ==================== 智能体管理 ====================

    /**
     * 获取智能体分页列表
     */
    @GetMapping("/agents")
    public BaseResponse<AdminPageResult<AgentResponse>> getAgents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        List<AgentResponse> agentList = agentService.getAgentListByPage(page, pageSize);
        Long total = agentService.getAgentNum();
        
        AdminPageResult<AgentResponse> result = AdminPageResult.<AgentResponse>builder()
                .list(agentList)
                .total(total)
                .page(page)
                .pageSize(pageSize)
                .build();
        
        return ResultUtils.success(result);
    }

    /**
     * 删除智能体
     */
    @DeleteMapping("/agents/{agentId}")
    public BaseResponse<Boolean> deleteAgent(
            @PathVariable Long agentId,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        User loginUser = userService.getLoginUser(session);
        Boolean result = agentService.deleteAgent(agentId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 更新智能体状态
     */
    @PutMapping("/agents/{agentId}/status")
    public BaseResponse<Boolean> updateAgentStatus(
            @PathVariable Long agentId,
            @RequestParam Integer status,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        User loginUser = userService.getLoginUser(session);
        Boolean result = agentService.updateAgentStatus(agentId, status, loginUser);
        return ResultUtils.success(result);
    }

    // ==================== 工具管理 ====================

    /**
     * 获取工具分页列表
     */
    @GetMapping("/tools")
    public BaseResponse<AdminPageResult<Tools>> getTools(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        List<Tools> toolList = toolService.getAllTools(page, pageSize);
        Long total = toolService.getToolsCount();
        
        AdminPageResult<Tools> result = AdminPageResult.<Tools>builder()
                .list(toolList)
                .total(total)
                .page(page)
                .pageSize(pageSize)
                .build();
        
        return ResultUtils.success(result);
    }

    /**
     * 删除工具
     */
    @DeleteMapping("/tools/{toolId}")
    public BaseResponse<Boolean> deleteTool(
            @PathVariable Long toolId,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        User loginUser = userService.getLoginUser(session);
        Boolean result = toolService.deleteTool(toolId, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新工具状态
     */
    @PutMapping("/tools/{toolId}/status")
    public BaseResponse<Boolean> updateToolStatus(
            @PathVariable Long toolId,
            @RequestParam Boolean isEnabled,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        Tools tool = toolService.getToolById(toolId, null);
        if (tool == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "工具不存在");
        }
        
        tool.setIsEnabled(isEnabled);
        User loginUser = userService.getLoginUser(session);
        Boolean result = toolService.updateTool(tool, loginUser.getId());
        return ResultUtils.success(result);
    }

    // ==================== 知识库管理 ====================

    /**
     * 获取知识库分页列表
     */
    @GetMapping("/knowledge-bases")
    public BaseResponse<AdminPageResult<KnowledgeBase>> getKnowledgeBases(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        Page<KnowledgeBase> kbPage = knowledgeBaseService.getKnowledgeBasesPage(page, pageSize);
        
        AdminPageResult<KnowledgeBase> result = AdminPageResult.<KnowledgeBase>builder()
                .list(kbPage.getRecords())
                .total(kbPage.getTotal())
                .page(page)
                .pageSize(pageSize)
                .build();
        
        return ResultUtils.success(result);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/knowledge-bases/{kbId}")
    public BaseResponse<Boolean> deleteKnowledgeBase(
            @PathVariable Long kbId,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        User loginUser = userService.getLoginUser(session);
        Boolean result = knowledgeBaseService.deleteKnowledgeBase(kbId, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 获取知识库文档列表
     */
    @GetMapping("/knowledge-bases/{kbId}/documents")
    public BaseResponse<AdminPageResult<KnowledgeBaseDocument>> getKnowledgeBaseDocuments(
            @PathVariable Long kbId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        Page<KnowledgeBaseDocument> docPage = knowledgeBaseService.getDocumentsPage(kbId, page, pageSize);
        
        AdminPageResult<KnowledgeBaseDocument> result = AdminPageResult.<KnowledgeBaseDocument>builder()
                .list(docPage.getRecords())
                .total(docPage.getTotal())
                .page(page)
                .pageSize(pageSize)
                .build();
        
        return ResultUtils.success(result);
    }

    // ==================== AI 模型管理 ====================

    /**
     * 获取 AI 模型分页列表
     */
    @GetMapping("/ai-models")
    public BaseResponse<AdminPageResult<AiModel>> getAiModels(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        Page<AiModel> modelPage = aiModelService.getAiModelsPage(page, pageSize);
        
        AdminPageResult<AiModel> result = AdminPageResult.<AiModel>builder()
                .list(modelPage.getRecords())
                .total(modelPage.getTotal())
                .page(page)
                .pageSize(pageSize)
                .build();
        
        return ResultUtils.success(result);
    }

    /**
     * 删除 AI 模型
     */
    @DeleteMapping("/ai-models/{modelId}")
    public BaseResponse<Boolean> deleteAiModel(
            @PathVariable Long modelId,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        User loginUser = userService.getLoginUser(session);
        Boolean result = aiModelService.deleteAiModel(modelId, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新 AI 模型状态
     */
    @PutMapping("/ai-models/{modelId}/status")
    public BaseResponse<Boolean> updateAiModelStatus(
            @PathVariable Long modelId,
            @RequestParam Integer status,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        User loginUser = userService.getLoginUser(session);
        Boolean result = aiModelService.updateAiModelStatus(modelId, status, loginUser);
        return ResultUtils.success(result);
    }

    // ==================== MCP 服务器管理 ====================

    /**
     * 获取 MCP 服务器分页列表
     */
    @GetMapping("/mcp-servers")
    public BaseResponse<AdminPageResult<McpServers>> getMcpServers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        Page<McpServers> serverPage = mcpServerService.getMcpServersPage(page, pageSize);
        
        AdminPageResult<McpServers> result = AdminPageResult.<McpServers>builder()
                .list(serverPage.getRecords())
                .total(serverPage.getTotal())
                .page(page)
                .pageSize(pageSize)
                .build();
        
        return ResultUtils.success(result);
    }

    /**
     * 删除 MCP 服务器
     */
    @DeleteMapping("/mcp-servers/{serverId}")
    public BaseResponse<Boolean> deleteMcpServer(
            @PathVariable Long serverId,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        User loginUser = userService.getLoginUser(session);
        Boolean result = mcpServerService.deleteMcpServer(serverId, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新 MCP 服务器状态
     */
    @PutMapping("/mcp-servers/{serverId}/status")
    public BaseResponse<Boolean> updateMcpServerStatus(
            @PathVariable Long serverId,
            @RequestParam Integer status,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        User loginUser = userService.getLoginUser(session);
        Boolean result = mcpServerService.updateMcpServerStatus(serverId, status, loginUser);
        return ResultUtils.success(result);
    }

    // ==================== 模型提供商管理 ====================

    /**
     * 获取模型提供商分页列表
     */
    @GetMapping("/model-providers")
    public BaseResponse<AdminPageResult<ModelProvider>> getModelProviders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        Page<ModelProvider> providerPage = modelProviderService.getModelProvidersPage(page, pageSize);
        
        AdminPageResult<ModelProvider> result = AdminPageResult.<ModelProvider>builder()
                .list(providerPage.getRecords())
                .total(providerPage.getTotal())
                .page(page)
                .pageSize(pageSize)
                .build();
        
        return ResultUtils.success(result);
    }

    /**
     * 删除模型提供商
     */
    @DeleteMapping("/model-providers/{providerId}")
    public BaseResponse<Boolean> deleteModelProvider(
            @PathVariable Long providerId,
            HttpSession session
    ) {
        checkAdminPermission(session);

        Boolean result = modelProviderService.deleteModelProvider(providerId, session);
        return ResultUtils.success(result);
    }

    /**
     * 更新模型提供商状态
     */
    @PutMapping("/model-providers/{providerId}/status")
    public BaseResponse<Boolean> updateModelProviderStatus(
            @PathVariable Long providerId,
            @RequestParam Integer status,
            HttpSession session
    ) {
        checkAdminPermission(session);
        
        User loginUser = userService.getLoginUser(session);
        Boolean result = modelProviderService.updateModelProviderStatus(providerId, status, loginUser);
        return ResultUtils.success(result);
    }
    
    // ==================== 系统字典管理 ====================
    
    /**
     * 创建系统字典
     */
    @GetMapping("/sys-dict/add")
    public BaseResponse<Long> addDict(
            @org.springframework.web.bind.annotation.RequestBody com.esdllm.agentmesh.model.domain.SysDict sysDict,
            HttpSession session
    ) {
        checkAdminPermission(session);
        Long dictId = sysDictService.createDict(sysDict);
        return ResultUtils.success(dictId);
    }

    /**
     * 更新系统字典
     */
    @PutMapping("/sys-dict/update")
    public BaseResponse<Boolean> updateDict(
            @org.springframework.web.bind.annotation.RequestBody com.esdllm.agentmesh.model.domain.SysDict sysDict,
            HttpSession session
    ) {
        checkAdminPermission(session);
        Boolean result = sysDictService.updateDict(sysDict);
        return ResultUtils.success(result);
    }

    /**
     * 删除系统字典
     */
    @DeleteMapping("/sys-dict/delete/{dictId}")
    public BaseResponse<Boolean> deleteDict(
            @PathVariable Long dictId,
            HttpSession session
    ) {
        checkAdminPermission(session);
        Boolean result = sysDictService.deleteDict(dictId);
        return ResultUtils.success(result);
    }

    /**
     * 根据类型获取字典列表
     */
    @GetMapping("/sys-dict/list/{dictType}")
    public BaseResponse<List<com.esdllm.agentmesh.model.domain.SysDict>> getDictsByType(
            @PathVariable String dictType,
            HttpSession session
    ) {
        checkAdminPermission(session);
        List<com.esdllm.agentmesh.model.domain.SysDict> dictList = sysDictService.getDictsByType(dictType);
        return ResultUtils.success(dictList);
    }

    /**
     * 根据Key获取字典
     */
    @GetMapping("/sys-dict/get-by-key")
    public BaseResponse<com.esdllm.agentmesh.model.domain.SysDict> getDictByKey(
            @RequestParam String dictType,
            @RequestParam Integer dictKey,
            HttpSession session
    ) {
        checkAdminPermission(session);
        com.esdllm.agentmesh.model.domain.SysDict sysDict = sysDictService.getDictByKey(dictType, dictKey);
        return ResultUtils.success(sysDict);
    }

    /**
     * 获取所有字典
     */
    @GetMapping("/sys-dict/all")
    public BaseResponse<List<com.esdllm.agentmesh.model.domain.SysDict>> getAllDicts(HttpSession session) {
        checkAdminPermission(session);
        List<com.esdllm.agentmesh.model.domain.SysDict> dictList = sysDictService.getAllDicts();
        return ResultUtils.success(dictList);
    }
}
