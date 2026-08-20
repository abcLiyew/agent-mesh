package com.esdllm.agentmesh.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 对话响应结果
 */
@Data
@Schema(description = "对话响应结果")
public class ChatResponse {
    
    @Schema(description = "最终回答内容")
  private String answer;
    
    @Schema(description= "决策路径")
  private Object decisionPath;
    
    @Schema(description= "内部决策使用的模型")
  private String internalModel;
    
    @Schema(description= "最终回答使用的模型")
  private String responseModel;
    
    @Schema(description= "执行耗时（毫秒）")
  private Long executionTime;
    
    @Schema(description = "是否成功")
  private Boolean success;
    
    @Schema(description = "错误信息")
  private String errorMessage;
}
