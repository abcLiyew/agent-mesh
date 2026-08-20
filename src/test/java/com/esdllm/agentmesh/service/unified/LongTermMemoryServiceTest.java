package com.esdllm.agentmesh.service.unified;

import com.esdllm.agentmesh.model.domain.AgentLongTermMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 长期记忆服务测试
 */
@SpringBootTest
@Slf4j
public class LongTermMemoryServiceTest {
    
    @Resource
    private LongTermMemoryService memoryService;
    
    @Test
    public void testStoreAndRetrieveMemory() {
        // 1. 存储记忆
        AgentLongTermMemory memory = new AgentLongTermMemory();
        memory.setUserId(1L);
        memory.setAgentId(1L);
        memory.setMemoryType("USER_PREFERENCE");
        memory.setContent("用户偏好简洁的回答风格，不喜欢冗长的解释");
        memory.setImportance(7);
        
        List<String> tags = Arrays.asList("preference", "style");
        memory.setTagsJson(tags);
        
        Long memoryId = memoryService.storeMemory(memory);
        assertNotNull(memoryId);
        log.info("记忆存储成功，ID: {}", memoryId);
        
        // 2. 检索记忆
        List<AgentLongTermMemory> memories = memoryService.retrieveMemories(
            1L, 1L, "回答风格", Arrays.asList("USER_PREFERENCE"), 5
        );
        
        assertFalse(memories.isEmpty());
        assertEquals(1, memories.size());
        assertTrue(memories.get(0).getContent().contains("简洁"));
        
        log.info("检索到 {} 条记忆", memories.size());
    }
    
    @Test
    public void testMemoryAccessTracking() {
        // 1. 创建记忆
        AgentLongTermMemory memory = new AgentLongTermMemory();
        memory.setUserId(1L);
        memory.setMemoryType("PROJECT_CONTEXT");
        memory.setContent("项目使用Spring Boot + Vue技术栈");
        memory.setImportance(8);
        
        Long memoryId = memoryService.storeMemory(memory);
        
        // 2. 记录多次访问
        for (int i = 0; i < 5; i++) {
            memoryService.recordMemoryAccess(memoryId);
        }
        
        // 3. 验证访问计数
        List<AgentLongTermMemory> memories = memoryService.retrieveMemories(
            1L, null, "", null, 10
        );
        
        Optional<AgentLongTermMemory> targetMemory = memories.stream()
            .filter(m -> m.getId().equals(memoryId))
            .findFirst();
        
        assertTrue(targetMemory.isPresent());
        assertEquals(5, targetMemory.get().getAccessCount());
        
        log.info("记忆访问计数: {}", targetMemory.get().getAccessCount());
    }
    
    @Test
    public void testUserProfileGeneration() {
        // 1. 存储多条用户偏好
        String[] preferences = {
            "喜欢深色主题",
            "偏好中文界面",
            "经常查询订单信息",
            "关注价格优惠"
        };
        
        for (String pref : preferences) {
            AgentLongTermMemory memory = new AgentLongTermMemory();
            memory.setUserId(2L);
            memory.setMemoryType("USER_PREFERENCE");
            memory.setContent(pref);
            memory.setImportance(5);
            memoryService.storeMemory(memory);
        }
        
        // 2. 获取用户画像
        Map<String, Object> profile = memoryService.getUserProfile(2L);
        
        assertNotNull(profile);
        assertTrue(profile.containsKey("preferences"));
        assertTrue(profile.containsKey("totalMemories"));
        
        log.info("用户画像: {}", profile);
    }
    
    @Test
    public void testMemoryExpiration() {
        // 1. 创建带过期时间的记忆
        AgentLongTermMemory memory = new AgentLongTermMemory();
        memory.setUserId(1L);
        memory.setMemoryType("INTERACTION_HISTORY");
        memory.setContent("临时对话内容");
        memory.setExpiresAt(new Date(System.currentTimeMillis() - 1000)); // 已过期
        
        Long memoryId = memoryService.storeMemory(memory);
        
        // 2. 验证过期记忆不被检索
        List<AgentLongTermMemory> memories = memoryService.retrieveMemories(
            1L, null, "", null, 10
        );
        
        boolean hasExpired = memories.stream()
            .anyMatch(m -> m.getId().equals(memoryId));
        
        assertFalse(hasExpired, "过期记忆不应被检索到");
        
        log.info("过期记忆过滤测试通过");
    }
}
