package com.esdllm.agentmesh.service;


import com.esdllm.agentmesh.model.dto.CostAlertNotification;

/**
 * 通知服务接口
 */
public interface NotificationService {
    
    /**
     * 发送成本告警通知
     * @param notification 告警通知信息
     */
    void sendCostAlert(CostAlertNotification notification);
    
    /**
     * 发送邮件通知
     * @param to 收件人
     * @param subject 主题
     * @param content 内容
     */
    void sendEmail(String to, String subject, String content);
    
    /**
     * 发送 Webhook 通知
     * @param webhookUrl Webhook 地址
     * @param data 数据
     */
    void sendWebhook(String webhookUrl, Object data);
}
