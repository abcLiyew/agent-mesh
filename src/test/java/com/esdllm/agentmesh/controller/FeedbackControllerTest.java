package com.esdllm.agentmesh.controller;


import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.FeedbackStatistics;
import com.esdllm.agentmesh.model.dto.request.FeedbackRequest;
import com.esdllm.agentmesh.service.ConversationLogService;
import com.esdllm.agentmesh.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FeedbackController 单元测试
 */
class FeedbackControllerTest {
    @Resource
    private UserService userService;

    private MockMvc mockMvc;

    private ConversationLogService conversationLogService;

    @Mock
    private MockHttpSession session;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        openMocks(this);

        // 创建 Mock 服务
        conversationLogService = org.mockito.Mockito.mock(ConversationLogService.class);

        // 使用 standaloneSetup 并注入 Mock 服务
        mockMvc = MockMvcBuilders.standaloneSetup(new FeedbackController())
                .build();

        // 模拟登录用户
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        when(userService.getLoginUser(session)).thenReturn(mockUser);
    }
    @Test
    void testSubmitFeedback_Success() throws Exception {
        // 准备测试数据
        FeedbackRequest request = new FeedbackRequest();
        request.setLogId(1L);
        request.setRating(5);
        request.setFeedback("很好的回答！");
        
        // 模拟服务行为
        doNothing().when(conversationLogService).updateFeedback(eq(1L), eq(5), any());
        
        // 执行测试
        mockMvc.perform(post("/api/feedback/submit")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void testGetMyFeedbackStatistics() throws Exception {
        // 准备测试数据 - 使用 Builder 模式创建
        FeedbackStatistics.RatingDistribution distribution = FeedbackStatistics.RatingDistribution.builder()
                .oneStar(2)
                .twoStar(3)
                .threeStar(15)
                .fourStar(30)
                .fiveStar(50)
                .build();

        FeedbackStatistics mockStats = FeedbackStatistics.builder()
                .totalFeedbacks(100L)
                .averageRating(4.5)
                .positiveFeedbacks(80L)
                .neutralFeedbacks(15L)
                .negativeFeedbacks(5L)
                .positiveRate(80.0)
                .negativeRate(5.0)
                .distribution(distribution)
                .build();

        when(conversationLogService.getUserFeedbackStats(1L)).thenReturn(mockStats);

        // 执行测试
        mockMvc.perform(get("/api/feedback/my-statistics")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalFeedbacks").value(100))
                .andExpect(jsonPath("$.data.averageRating").value(4.5));
    }


    @Test
    void testSubmitFeedback_Unauthorized() throws Exception {
        FeedbackRequest request = new FeedbackRequest();
        request.setLogId(999L);
        request.setRating(3);
        
        // 模拟非授权访问
        when(conversationLogService.getFeedbackDetail(999L))
                .thenThrow(new RuntimeException("无权访问"));
        
        mockMvc.perform(post("/api/feedback/submit")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }
}
