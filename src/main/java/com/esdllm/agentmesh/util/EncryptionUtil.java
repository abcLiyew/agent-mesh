package com.esdllm.agentmesh.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 加密工具类
 * 提供 AES 加密和解密功能
 */
@Component
@Slf4j
public class EncryptionUtil {

    /**
     * AES 密钥（16 位、24 位或 32 位，对应 AES-128、AES-192、AES-256）
     * 从配置文件中读取
     */
    private static String aesKey;

    @Value("${encryption.aes-key:AgentMesh2026Secret}")
    public void setAesKey(String key) {
        // 确保密钥长度为 16 的倍数（AES 要求）
        if (key.length() < 16) {
            aesKey = String.format("%-16s", key).replace(' ', '0');
        } else if (key.length() < 24) {
            aesKey = key.substring(0, 16);
        } else if (key.length() < 32) {
            aesKey = key.substring(0, 24);
        } else {
            aesKey = key.substring(0, 32);
        }
        log.info("AES 密钥已加载，长度：{}", aesKey.length());
    }

    /**
     * AES 加密
     * @param plainText 明文
     * @return Base64 编码的密文
     */
    public static String encrypt(String plainText) {
        try {
            if (plainText == null || plainText.isEmpty()) {
                return "";
            }
            
            SecretKeySpec keySpec = new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getEncoder().encodeToString(encryptedBytes);
            
            log.debug("AES 加密成功，原文长度：{}, 密文长度：{}", plainText.length(), encoded.length());
            return encoded;
            
        } catch (Exception e) {
            log.error("AES 加密失败", e);
            throw new RuntimeException("加密失败：" + e.getMessage(), e);
        }
    }

    /**
     * AES 解密
     * @param cipherText Base64 编码的密文
     * @return 明文
     */
    public static String decrypt(String cipherText) {
        try {
            if (cipherText == null || cipherText.isEmpty()) {
                return "";
            }
            
            SecretKeySpec keySpec = new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            
            byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            
            String decrypted = new String(decryptedBytes, StandardCharsets.UTF_8);
            log.debug("AES 解密成功，密文长度：{}, 原文长度：{}", cipherText.length(), decrypted.length());
            
            return decrypted;
            
        } catch (Exception e) {
            log.error("AES 解密失败", e);
            throw new RuntimeException("解密失败：" + e.getMessage(), e);
        }
    }

    /**
     * 判断字符串是否为 Base64 编码
     * @param str 待检查的字符串
     * @return 是否为 Base64
     */
    public static boolean isBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
