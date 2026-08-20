package com.esdllm.agentmesh.service;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Token 计数工具类
 * 用于估算文本的 Token 数量
 */
@Component
@Slf4j
public class TokenCounter {

    /**
     * 中文字符平均长度（估算）
     */
    private static final double AVG_CHINESE_CHAR_LENGTH = 1.5;
    
    /**
     * 英文字符平均长度
     */
    private static final double AVG_ENGLISH_CHAR_LENGTH = 0.75;

    @PostConstruct
    public void init() {
        log.info("Token 计数器初始化完成");
    }

    /**
     * 估算文本的 Token 数量
     * @param text 输入文本
     * @return Token 数量
     */
    public int countTokens(String text) {
        if (StrUtil.isBlank(text)) {
            return 0;
        }
        
        // 简单的估算方法：
        // 1. 中文字符：每 2 个字符约等于 1 个 Token
        // 2. 英文字符：每 4 个字符约等于 1 个 Token
        // 3. 混合文本：按比例估算
        
        int chineseChars = 0;
        int englishChars = 0;
        
        for (char c : text.toCharArray()) {
            if (isChinese(c)) {
                chineseChars++;
            } else if (isEnglishLetter(c)) {
                englishChars++;
            }
        }
        
        // 估算 Token 数
        int tokens = (int) Math.ceil(chineseChars / AVG_CHINESE_CHAR_LENGTH) + 
                    (int) Math.ceil(englishChars / AVG_ENGLISH_CHAR_LENGTH);
        
        // 加上标点符号和空格的估算
        tokens += (int) Math.ceil((text.length() - chineseChars - englishChars) / 4.0);
        
        log.debug("文本长度：{}, 中文：{}, 英文：{}, 估算 Token: {}", 
            text.length(), chineseChars, englishChars, tokens);
        
        return tokens;
    }

    /**
     * 计算 API 调用成本
     * @param inputTokens 输入 Token 数
     * @param outputTokens 输出 Token 数
     * @param inputCostPer1k 每千输入 Token 成本
     * @param outputCostPer1k 每千输出 Token 成本
     * @return 总成本
     */
    public BigDecimal calculateCost(int inputTokens, int outputTokens, 
                                   BigDecimal inputCostPer1k, BigDecimal outputCostPer1k) {
        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
            .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
            .multiply(inputCostPer1k);
        
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
            .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
            .multiply(outputCostPer1k);
        
        return inputCost.add(outputCost);
    }

    /**
     * 判断是否为中文字符
     */
    private boolean isChinese(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
    }

    /**
     * 判断是否为英文字母
     */
    private boolean isEnglishLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}
