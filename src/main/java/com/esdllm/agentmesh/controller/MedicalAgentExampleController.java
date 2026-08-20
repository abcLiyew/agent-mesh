package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.service.UserService;
import com.esdllm.agentmesh.service.impl.MedicalAgentExampleService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 医疗助手示例控制器
 * 演示如何快速创建医疗领域的知识库和智能体
 */
@RestController
@RequestMapping("/example/medical")
@Slf4j
public class MedicalAgentExampleController {

    @Resource
    private MedicalAgentExampleService medicalAgentExampleService;

    @Resource
    private UserService userService;

    /**
     * 一键创建医疗助手系统
     * 自动创建：知识库 + 示例文档 + 智能体 + 关联关系
     * 
     * @param request HTTP请求
     * @param embeddingModelId 嵌入模型ID（可选，默认使用第一个可用的嵌入模型）
     * @param decisionModelId 决策模型ID（可选，默认使用第一个可用的聊天模型）
     * @param responseModelId 回复模型ID（可选，默认使用第一个可用的聊天模型）
     * @return 创建的智能体ID和知识库ID
     */
    @PostMapping("/create-system")
    public BaseResponse<Map<String, Object>> createMedicalSystem(
            HttpServletRequest request,
            @RequestParam(required = false) Long embeddingModelId,
            @RequestParam(required = false) Long decisionModelId,
            @RequestParam(required = false) Long responseModelId
    ) {
        try {
            // 获取当前登录用户
            User loginUser = userService.getLoginUser(request.getSession());
            if (loginUser == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }

            log.info("用户 {} 开始创建医疗助手系统", loginUser.getId());

            // 如果未提供模型ID，可以使用默认值或查询可用模型
            // 这里简化处理，实际应用中应该查询数据库获取可用模型
            if (embeddingModelId == null) {
                embeddingModelId = 3L; // 假设的嵌入模型ID
            }
            if (decisionModelId == null) {
                decisionModelId = 1L; // 假设的决策模型ID
            }
            if (responseModelId == null) {
                responseModelId = 2L; // 假设的回复模型ID
            }

            // 创建医疗助手系统
            Long agentId = medicalAgentExampleService.createMedicalAssistantSystem(
                    loginUser.getId(),
                    embeddingModelId,
                    decisionModelId,
                    responseModelId
            );

            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("agentId", agentId);
            result.put("message", "医疗助手系统创建成功！");
            result.put("nextSteps", new String[]{
                    "1. 访问 /agent/" + agentId + " 查看智能体详情",
                    "2. 访问 /knowledge-base/my-list 查看知识库",
                    "3. 使用 /unified-agent/execute 测试智能体"
            });

            return ResultUtils.success(result);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建医疗助手系统失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建失败：" + e.getMessage());
        }
    }

    /**
     * 添加专业化医疗文档到现有知识库
     * 
     * @param kbId 知识库ID
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/add-specialized-docs/{kbId}")
    public BaseResponse<Map<String, Object>> addSpecializedDocuments(
            @PathVariable Long kbId,
            HttpServletRequest request
    ) {
        try {
            // 获取当前登录用户
            User loginUser = userService.getLoginUser(request.getSession());
            if (loginUser == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }

            log.info("用户 {} 向知识库 {} 添加专业化文档", loginUser.getId(), kbId);

            // 添加专业化文档
            medicalAgentExampleService.addSpecializedDocuments(kbId);

            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("kbId", kbId);
            result.put("message", "专业化医疗文档添加成功！");
            result.put("addedDocuments", new String[]{
                    "心血管健康管理指南",
                    "糖尿病患者自我管理手册"
            });

            return ResultUtils.success(result);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("添加专业化文档失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加失败：" + e.getMessage());
        }
    }

    /**
     * 获取医疗助手使用示例
     * 
     * @return 使用示例列表
     */
    @GetMapping("/usage-examples")
    public BaseResponse<Map<String, Object>> getUsageExamples() {
        Map<String, Object> examples = new HashMap<>();
        
        examples.put("description", "医疗助手使用示例");
        examples.put("examples", new Object[]{
            new HashMap<String, String>() {{
                put("query", "我最近经常头痛，可能是什么原因？");
                put("description", "常见症状咨询");
            }},
            new HashMap<String, String>() {{
                put("query", "高血压患者应该注意什么？");
                put("description", "慢性病管理建议");
            }},
            new HashMap<String, String>() {{
                put("query", "什么是BMI指数，如何计算？");
                put("description", "健康指标解释");
            }},
            new HashMap<String, String>() {{
                put("query", "感冒和流感有什么区别？");
                put("description", "疾病知识科普");
            }},
            new HashMap<String, String>() {{
                put("query", "每天应该喝多少水？");
                put("description", "健康生活方式建议");
            }}
        });
        
        examples.put("disclaimer", "重要声明：本助手提供的信息仅供参考和教育目的，不能替代专业医疗建议、诊断或治疗。如有健康问题，请咨询合格的医疗专业人员。");

        return ResultUtils.success(examples);
    }
}