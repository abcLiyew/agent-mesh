package com.esdllm.agentmesh.service.impl;

import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.model.domain.AgentKbRelation;
import com.esdllm.agentmesh.model.domain.KnowledgeBase;
import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.repository.dao.AgentKbRelationDao;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDao;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDocumentDao;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 示例服务：演示如何以编程方式创建医疗领域知识库和智能体
 */
@Service
@Slf4j
public class MedicalAgentExampleService {

    @Resource
    private KnowledgeBaseDao knowledgeBaseDao;

    @Resource
    private KnowledgeBaseDocumentDao knowledgeBaseDocumentDao;

    @Resource
    private AgentDao agentDao;

    @Resource
    private AgentKbRelationDao agentKbRelationDao;

    /**
     * 创建完整的医疗助手系统（知识库 + 智能体 + 关联）
     * 
     * @param userId 用户ID
     * @param embeddingModelId 嵌入模型ID
     * @param decisionModelId 决策模型ID
     * @param responseModelId 回复模型ID
     * @return 创建的智能体ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createMedicalAssistantSystem(
            Long userId,
            Long embeddingModelId,
            Long decisionModelId,
            Long responseModelId
    ) {
        log.info("开始创建医疗助手系统，用户ID: {}", userId);

        // 1. 创建医疗知识库
        Long kbId = createMedicalKnowledgeBase(userId, embeddingModelId);
        log.info("医疗知识库创建成功，ID: {}", kbId);

        // 2. 添加示例文档
        addSampleDocuments(kbId);
        log.info("示例文档添加成功");

        // 3. 创建医疗助手智能体
        Long agentId = createMedicalAssistantAgent(
                userId, 
                decisionModelId, 
                responseModelId
        );
        log.info("医疗助手智能体创建成功，ID: {}", agentId);

        // 4. 建立智能体与知识库的关联
        linkAgentToKnowledgeBase(agentId, kbId);
        log.info("智能体与知识库关联成功");

        log.info("医疗助手系统创建完成！智能体ID: {}, 知识库ID: {}", agentId, kbId);
        return agentId;
    }

    /**
     * 创建医疗知识库
     */
    private Long createMedicalKnowledgeBase(Long userId, Long embeddingModelId) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setUserId(userId);
        kb.setName("医疗健康知识库");
        kb.setDescription("包含常见疾病、症状、治疗方案等医疗信息的知识库，用于辅助医疗问答和健康咨询");
        kb.setVectorStoreType("OLLAMA");
        kb.setVectorStoreTable("ollama_vector_store");
        kb.setEmbeddingModelId(embeddingModelId);
        kb.setChunkSize(500);
        kb.setChunkOverlap(50);
        kb.setStatus(1); // 启用
        kb.setIsDelete(0);
        kb.setCreatedAt(new Date());
        kb.setUpdatedAt(new Date());
        // visibility: 0=私有, 1=团队共享, 2=公开
        // 注意：KnowledgeBase 实体类中可能没有 visibility 字段，需要根据实际情况调整

        knowledgeBaseDao.save(kb);
        return kb.getId();
    }

    /**
     * 添加示例文档到知识库
     */
    private void addSampleDocuments(Long kbId) {
        // 文档 1: 常见疾病诊疗指南
        KnowledgeBaseDocument doc1 = new KnowledgeBaseDocument();
        doc1.setKbId(kbId);
        doc1.setDocName("常见疾病诊疗指南");
        doc1.setDocType("TEXT");
        doc1.setContentHash("hash_common_diseases_001");
        doc1.setChunkCount(0);
        doc1.setStatus(1); // 处理完成
        doc1.setIsDelete(0);
        doc1.setCreatedAt(new Date());
        doc1.setUpdatedAt(new Date());
        // metadataJson 和 relatedToolIds 可能需要使用 JSON 序列化工具
        knowledgeBaseDocumentDao.save(doc1);

        // 文档 2: 常用药物说明书
        KnowledgeBaseDocument doc2 = new KnowledgeBaseDocument();
        doc2.setKbId(kbId);
        doc2.setDocName("常用药物说明书");
        doc2.setDocType("TEXT");
        doc2.setContentHash("hash_medications_001");
        doc2.setChunkCount(0);
        doc2.setStatus(1);
        doc2.setIsDelete(0);
        doc2.setCreatedAt(new Date());
        doc2.setUpdatedAt(new Date());
        knowledgeBaseDocumentDao.save(doc2);

        // 文档 3: 健康生活指南
        KnowledgeBaseDocument doc3 = new KnowledgeBaseDocument();
        doc3.setKbId(kbId);
        doc3.setDocName("健康生活指南");
        doc3.setDocType("TEXT");
        doc3.setContentHash("hash_health_lifestyle_001");
        doc3.setChunkCount(0);
        doc3.setStatus(1);
        doc3.setIsDelete(0);
        doc3.setCreatedAt(new Date());
        doc3.setUpdatedAt(new Date());
        knowledgeBaseDocumentDao.save(doc3);
    }

    /**
     * 创建医疗助手智能体
     */
    private Long createMedicalAssistantAgent(
            Long userId,
            Long decisionModelId,
            Long responseModelId
    ) {
        Agent agent = new Agent();
        agent.setUserId(userId);
        agent.setName("医疗健康助手");
        agent.setDescription("专业的医疗健康咨询助手，能够回答常见疾病、症状、治疗方案等问题");
        
        String systemPrompt = "你是一个专业的医疗健康助手，具备丰富的医学知识。你的主要职责是：\n" +
                "1. 回答用户关于常见疾病的疑问\n" +
                "2. 提供健康生活方式建议\n" +
                "3. 解释医学术语和检查结果\n" +
                "4. 给出一般性的健康指导\n\n" +
                "重要原则：\n" +
                "- 始终强调你提供的信息仅供参考，不能替代专业医生的诊断和治疗\n" +
                "- 对于紧急或严重情况，建议用户立即就医\n" +
                "- 使用通俗易懂的语言解释医学概念\n" +
                "- 保持专业、准确、负责任的态度\n" +
                "- 不提供具体的药物剂量建议，只说明一般用途\n" +
                "- 尊重用户隐私，不询问敏感个人信息";
        
        agent.setSystemPrompt(systemPrompt);
        agent.setRoleDefinition("医疗健康领域的专业顾问，具有内科、全科医学背景，擅长健康咨询和疾病预防指导");
        agent.setDecisionModelId(decisionModelId);
        agent.setResponseModelId(responseModelId);
        agent.setIsToolEnabled(false);
        agent.setVersion("1.0.0");
        agent.setStatus(1); // 发布
        agent.setIsDelete(0);
        agent.setCreatedAt(new Date());
        agent.setUpdatedAt(new Date());
        // visibility 和其他字段根据实际需求设置

        agentDao.save(agent);
        return agent.getId();
    }

    /**
     * 建立智能体与知识库的关联
     */
    private void linkAgentToKnowledgeBase(Long agentId, Long kbId) {
        AgentKbRelation relation = new AgentKbRelation();
        relation.setAgentId(agentId);
        relation.setKbId(kbId);
        relation.setSearchTopK(5); // 检索返回的最大结果数
        relation.setSimilarityThreshold(new BigDecimal("0.7")); // 相似度阈值
        relation.setSortOrder(1);
        relation.setIsDelete(0);
        relation.setCreatedAt(new Date());
        relation.setUpdatedAt(new Date());

        agentKbRelationDao.save(relation);
    }

    /**
     * 扩展示例：添加更多专业化的医疗文档
     */
    @Transactional(rollbackFor = Exception.class)
    public void addSpecializedDocuments(Long kbId) {
        // 心血管健康专题
        KnowledgeBaseDocument cardioDoc = new KnowledgeBaseDocument();
        cardioDoc.setKbId(kbId);
        cardioDoc.setDocName("心血管健康管理指南");
        cardioDoc.setDocType("TEXT");
        cardioDoc.setContentHash("hash_cardiovascular_001");
        cardioDoc.setChunkCount(0);
        cardioDoc.setStatus(1);
        cardioDoc.setIsDelete(0);
        cardioDoc.setCreatedAt(new Date());
        cardioDoc.setUpdatedAt(new Date());
        knowledgeBaseDocumentDao.save(cardioDoc);

        // 糖尿病管理
        KnowledgeBaseDocument diabetesDoc = new KnowledgeBaseDocument();
        diabetesDoc.setKbId(kbId);
        diabetesDoc.setDocName("糖尿病患者自我管理手册");
        diabetesDoc.setDocType("TEXT");
        diabetesDoc.setContentHash("hash_diabetes_001");
        diabetesDoc.setChunkCount(0);
        diabetesDoc.setStatus(1);
        diabetesDoc.setIsDelete(0);
        diabetesDoc.setCreatedAt(new Date());
        diabetesDoc.setUpdatedAt(new Date());
        knowledgeBaseDocumentDao.save(diabetesDoc);

        log.info("已添加专业化医疗文档到知识库: {}", kbId);
    }
}