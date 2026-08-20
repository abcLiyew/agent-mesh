package com.esdllm.agentmesh.service.agent;

import com.esdllm.agentmesh.model.dto.ToolInvocationContext;

/**
 * 工具调用器接口
 */
public interface ToolInvoker {
    
    /**
     * 调用工具
     * @param context 调用上下文
     * @return 工具执行结果
     */
   String invoke(ToolInvocationContext context);
    
    /**
     * 异步调用工具
     * @param context 调用上下文
     * @return 任务 ID
     */
   String invokeAsync(ToolInvocationContext context);
    
    /**
     * 检查工具是否可用
     * @param toolId 工具 ID
     * @return 是否可用
     */
    boolean isAvailable(Long toolId);
}
