package com.esdllm.agentmesh.service.impl;


import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.esdllm.agentmesh.model.dto.CostAlertNotification;
import com.esdllm.agentmesh.service.NotificationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;

/**
 * 通知服务实现
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    
    private static final DecimalFormat DF = new DecimalFormat("#.##");
    
    @Value("${spring.mail.username:}")
    private String mailFrom;
    
    @Value("${notification.webhook.timeout:5000}")
    private int webhookTimeout;

    @Resource
    private  JavaMailSender mailSender;

    
    @PostConstruct
    public void init() {
        log.info("通知服务初始化完成");
        if (mailSender == null) {
            log.warn("未配置 JavaMailSender，邮件发送功能将不可用");
        }
    }
    
    @Override
    public void sendCostAlert(CostAlertNotification notification) {
        log.info("发送成本告警通知：{}", notification);
        
        try {
            // 构建告警消息
            String message = buildAlertMessage(notification);
            
            // 根据通知方式发送
            String method = notification.getMessage();
            if (method != null && method.contains("EMAIL")) {
                sendEmail(notification.getUserId().toString(), "成本超阈值告警", message);
            } else if (method != null && method.contains("WEBHOOK")) {
                sendWebhook(notification.getMessage(), notification);
            } else {
                // 默认记录日志
                log.warn("成本告警：{}", message);
            }
            
        } catch (Exception e) {
            log.error("发送成本告警通知失败", e);
        }
    }
    
    @Override
    public void sendEmail(String to, String subject, String content) {
        if (mailSender == null) {
            log.warn("JavaMailSender 未配置，无法发送邮件 - 收件人：{}, 主题：{}", to, subject);
            log.info("模拟发送邮件 - 收件人：{}, 主题：{}, 内容：{}", to, subject, content);
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            
            log.info("邮件发送成功 - 收件人：{}, 主题：{}", to, subject);
            
        } catch (Exception e) {
            log.error("邮件发送失败 - 收件人：{}, 主题：{}", to, subject, e);
            throw new RuntimeException("邮件发送失败：" + e.getMessage(), e);
        }
    }
    
    @Override
    public void sendWebhook(String webhookUrl, Object data) {
        try {
            // 将数据转换为 JSON
            String jsonBody = JSONUtil.toJsonStr(data);
            
            // 发送 POST 请求
            String response = HttpRequest.post(webhookUrl)
                    .header("Content-Type", "application/json")
                    .body(jsonBody)
                    .timeout(webhookTimeout)
                    .execute()
                    .body();
            
            log.info("Webhook 发送成功 - URL: {}, 响应：{}", webhookUrl, response);
            
        } catch (Exception e) {
            log.error("Webhook 发送失败 - URL: {}", webhookUrl, e);
            throw new RuntimeException("Webhook 发送失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 构建告警消息
     */
    private String buildAlertMessage(CostAlertNotification notification) {
        StringBuilder sb = new StringBuilder();
        sb.append("【成本超阈值告警】\n");
        sb.append("用户 ID: ").append(notification.getUserId()).append("\n");
        
        if (notification.getAgentId() != null) {
            sb.append("智能体 ID: ").append(notification.getAgentId()).append("\n");
        }
        
        sb.append("告警类型：").append(notification.getAlertType()).append("\n");
        sb.append("当前成本：¥").append(DF.format(notification.getCurrentCost())).append("\n");
        sb.append("阈值：¥").append(DF.format(notification.getThreshold())).append("\n");
        sb.append("超出金额：¥").append(DF.format(notification.getExceededAmount())).append("\n");
        sb.append("超出百分比：").append(DF.format(notification.getExceededPercentage())).append("%\n");
        
        if (notification.getDowngradeTriggered() != null && notification.getDowngradeTriggered()) {
            sb.append("已触发降级策略：").append(notification.getDowngradeStrategy()).append("\n");
        }
        
        return sb.toString();
    }
}
