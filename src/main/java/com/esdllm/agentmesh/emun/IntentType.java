package com.esdllm.agentmesh.emun;

/**
 * 意图类型枚举
 */
public enum IntentType {
    
    /**
     * 产品查询
     */
    PRODUCT_QUERY,
    
    /**
     * 订单查询
     */
    ORDER_QUERY,
    
    /**
     * 知识问答
     */
    KNOWLEDGE_QA,
    
    /**
     * 工具调用
     */
    TOOL_CALL,
    
    /**
     * 智能体调用
     */
    AGENT_CALL,
    
    /**
     * 闲聊对话
     */
    CHAT,
    
    /**
     * 未知意图
     */
    UNKNOWN
}
