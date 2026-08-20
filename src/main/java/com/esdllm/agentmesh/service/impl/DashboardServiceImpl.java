package com.esdllm.agentmesh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.emun.UserRoleEnum;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.*;
import com.esdllm.agentmesh.model.dto.ActivityLog;
import com.esdllm.agentmesh.model.dto.DashboardStatistics;
import com.esdllm.agentmesh.model.dto.SystemStatus;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDocumentDao;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import com.esdllm.agentmesh.repository.dao.UserDao;
import com.esdllm.agentmesh.repository.dao.ConversationLogDao;
import com.esdllm.agentmesh.service.DashboardService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 系统管理后台服务实现类
 */
@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private UserDao userDao;

    @Resource
    private AgentDao agentDao;

    @Resource
    private ToolsDao toolsDao;

    @Resource
    private KnowledgeBaseDocumentDao knowledgeBaseDocumentDao;

    @Resource
    private ConversationLogDao conversationLogDao;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private UserService userService;

    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRE_MS = 60000;

    @Override
    public DashboardStatistics getDashboardStatistics(HttpSession session) {
        if (hasNotAdminAuthority(session)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        DashboardStatistics.UserStatistics userStats = getUserStatistics();
        DashboardStatistics.AgentStatistics agentStats = getAgentStatistics();
        DashboardStatistics.ToolStatistics toolStats = getToolStatistics();
        DashboardStatistics.KnowledgeBaseStatistics kbStats = getKnowledgeBaseStatistics();

        return DashboardStatistics.builder()
                .userStatistics(userStats)
                .agentStatistics(agentStats)
                .toolStatistics(toolStats)
                .knowledgeBaseStatistics(kbStats)
                .build();
    }

    @Override
    public SystemStatus getSystemStatus(HttpSession session) {
        if (hasNotAdminAuthority(session)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return SystemStatus.builder()
                .uptimeSeconds(getSystemUptime())
                .todayApiCalls(getTodayApiCalls())
                .averageResponseTimeMs(getAverageResponseTime())
                .errorRate(getErrorRate())
                .databaseStatus(checkDatabaseStatus())
                .cacheStatus(checkCacheStatus())
                .build();
    }

    @Override
    public List<ActivityLog> getRecentActivities(int limit, HttpSession session) {
        if (hasNotAdminAuthority(session)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        List<ActivityLog> activities = new ArrayList<>();

        try {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime startTime = LocalDateTime.now().minusHours(24);

            List<ActivityLog> allActivities = new ArrayList<>();

            allActivities.addAll(queryUserActivities(startTime, limit));
            allActivities.addAll(queryAgentActivities(startTime, limit));
            allActivities.addAll(queryToolActivities(startTime, limit));
            allActivities.addAll(queryMcpActivities(startTime, limit));
            allActivities.addAll(queryKbActivities(startTime, limit));

            allActivities.sort((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()));

            return allActivities.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取最近活动日志失败", e);
            return new ArrayList<>();
        }
    }

    private DashboardStatistics.UserStatistics getUserStatistics() {
        try {
            Long totalUsers = userDao.count();
            
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            Long todayNewUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"user\" WHERE created_at >= ? AND is_delete = 0",
                Long.class,
                todayStart
            );

            return DashboardStatistics.UserStatistics.builder()
                    .totalUsers(totalUsers)
                    .todayNewUsers(todayNewUsers != null ? todayNewUsers : 0L)
                    .build();
        } catch (Exception e) {
            log.error("获取用户统计失败", e);
            return DashboardStatistics.UserStatistics.builder()
                    .totalUsers(0L)
                    .todayNewUsers(0L)
                    .build();
        }
    }

    private DashboardStatistics.AgentStatistics getAgentStatistics() {
        try {
            Long totalAgents = agentDao.count();
            
            Long publishedAgents = agentDao.count(
                new LambdaQueryWrapper<Agent>()
                    .eq(Agent::getStatus, 1)
            );

            return DashboardStatistics.AgentStatistics.builder()
                    .totalAgents(totalAgents)
                    .publishedAgents(publishedAgents)
                    .build();
        } catch (Exception e) {
            log.error("获取智能体统计失败", e);
            return DashboardStatistics.AgentStatistics.builder()
                    .totalAgents(0L)
                    .publishedAgents(0L)
                    .build();
        }
    }

    private DashboardStatistics.ToolStatistics getToolStatistics() {
        try {
            Long totalTools = toolsDao.count();
            
            Long publicTools = toolsDao.count(
                new LambdaQueryWrapper<Tools>()
                    .eq(Tools::getIsEnabled, true)
                    .eq(Tools::getIsDelete, 0)
            );

            Long healthyTools = toolsDao.count(
                new LambdaQueryWrapper<Tools>()
                    .eq(Tools::getHealthStatus, 1)
                    .eq(Tools::getIsEnabled, true)
                    .eq(Tools::getIsDelete, 0)
            );

            return DashboardStatistics.ToolStatistics.builder()
                    .totalTools(totalTools)
                    .publicTools(publicTools)
                    .healthyTools(healthyTools)
                    .build();
        } catch (Exception e) {
            log.error("获取工具统计失败", e);
            return DashboardStatistics.ToolStatistics.builder()
                    .totalTools(0L)
                    .publicTools(0L)
                    .healthyTools(0L)
                    .build();
        }
    }

    private DashboardStatistics.KnowledgeBaseStatistics getKnowledgeBaseStatistics() {
        try {
            Long totalKnowledgeBases = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT kb_id) FROM knowledge_base_document WHERE is_delete = 0",
                Long.class
            );
            
            Long totalDocuments = knowledgeBaseDocumentDao.count(
                new LambdaQueryWrapper<KnowledgeBaseDocument>()
                    .eq(KnowledgeBaseDocument::getIsDelete, 0)
            );

            Long vectorizedDocuments = knowledgeBaseDocumentDao.count(
                new LambdaQueryWrapper<KnowledgeBaseDocument>()
                    .eq(KnowledgeBaseDocument::getIsDelete, 0)
                    .isNotNull(KnowledgeBaseDocument::getVectorIds)
            );

            return DashboardStatistics.KnowledgeBaseStatistics.builder()
                    .totalKnowledgeBases(totalKnowledgeBases != null ? totalKnowledgeBases : 0L)
                    .totalDocuments(totalDocuments)
                    .vectorizedDocuments(vectorizedDocuments)
                    .build();
        } catch (Exception e) {
            log.error("获取知识库统计失败", e);
            return DashboardStatistics.KnowledgeBaseStatistics.builder()
                    .totalKnowledgeBases(0L)
                    .totalDocuments(0L)
                    .vectorizedDocuments(0L)
                    .build();
        }
    }

    private Long getSystemUptime() {
        try {
            return ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        } catch (Exception e) {
            log.error("获取系统运行时间失败", e);
            return 0L;
        }
    }

    private Long getTodayApiCalls() {
        try {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            Long count = conversationLogDao.count(
                new LambdaQueryWrapper<ConversationLog>()
                    .ge(ConversationLog::getCreatedAt, todayStart)
            );
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("获取今日 API 调用次数失败", e);
            return 0L;
        }
    }

    private Double getAverageResponseTime() {
        try {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) * 1000) as avg_time FROM conversation_log WHERE created_at >= ?",
                todayStart
            );
            
            if (!results.isEmpty()) {
                Object avgTime = results.getFirst().get("avg_time");
                return avgTime != null ? ((Number) avgTime).doubleValue() : 0.0;
            }
            return 0.0;
        } catch (Exception e) {
            log.error("获取平均响应时间失败", e);
            return 0.0;
        }
    }

    private Double getErrorRate() {
        try {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            
            Long total = conversationLogDao.count(
                new LambdaQueryWrapper<ConversationLog>()
                    .ge(ConversationLog::getCreatedAt, todayStart)
            );
            
            if (total == 0) {
                return 0.0;
            }
            
            Long errorCount = conversationLogDao.count(
                new LambdaQueryWrapper<ConversationLog>()
                    .ge(ConversationLog::getCreatedAt, todayStart)
                    .eq(ConversationLog::getStatus, "ERROR")
            );
            
            return (double) errorCount / total * 100;
        } catch (Exception e) {
            log.error("获取错误率失败", e);
            return 0.0;
        }
    }

    private String checkDatabaseStatus() {
        try {
            jdbcTemplate.execute("SELECT 1");
            return "正常";
        } catch (Exception e) {
            log.error("数据库状态检查失败", e);
            return "异常";
        }
    }

    private String checkCacheStatus() {
        try {
            return "运行中";
        } catch (Exception e) {
            log.error("缓存状态检查失败", e);
            return "异常";
        }
    }

    private List<ActivityLog> queryUserActivities(LocalDateTime startTime, int limit) {
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT id, username, created_at FROM \"user\" WHERE created_at >= ? AND is_delete = 0 ORDER BY created_at DESC LIMIT ?",
                startTime,
                limit
            );
            
            return results.stream()
                .map(row -> ActivityLog.builder()
                        .id(((Number) row.get("id")).longValue())
                        .activityType("USER_REGISTER")
                        .title("新用户注册：" + row.get("username"))
                        .description("用户 " + row.get("username") + " 注册账号")
                        .status("SUCCESS")
                        .timestamp(((java.sql.Timestamp) row.get("created_at")).toLocalDateTime())
                        .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询用户活动失败", e);
            return new ArrayList<>();
        }
    }

    private List<ActivityLog> queryAgentActivities(LocalDateTime startTime, int limit) {
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT id, name, updated_at FROM agent WHERE updated_at >= ? AND status = 1 AND is_delete = 0 ORDER BY updated_at DESC LIMIT ?",
                startTime,
                limit
            );
            
            return results.stream()
                .map(row -> ActivityLog.builder()
                        .id(((Number) row.get("id")).longValue())
                        .activityType("AGENT_PUBLISH")
                        .title("智能体 \"" + row.get("name") + "\" 已发布")
                        .description("智能体 " + row.get("name") + " 发布成功")
                        .status("SUCCESS")
                        .timestamp(((java.sql.Timestamp) row.get("updated_at")).toLocalDateTime())
                        .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询智能体活动失败", e);
            return new ArrayList<>();
        }
    }

    private List<ActivityLog> queryToolActivities(LocalDateTime startTime, int limit) {
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT id, display_name, last_health_check, health_status FROM tools WHERE last_health_check >= ? AND is_delete = 0 ORDER BY last_health_check DESC LIMIT ?",
                startTime,
                limit
            );
            
            return results.stream()
                .map(row -> {
                    Integer healthStatus = (Integer) row.get("health_status");
                    String status = "SUCCESS";
                    String title = "工具 \"" + row.get("display_name") + "\" 状态更新";
                    String description = "工具 " + row.get("display_name") + " 健康状态正常";
                    
                    if (healthStatus != null) {
                        if (healthStatus == 2) {
                            status = "WARNING";
                            title = "工具 \"" + row.get("display_name") + "\" 响应缓慢";
                            description = "工具 " + row.get("display_name") + " 响应时间超过阈值";
                        } else if (healthStatus == 3) {
                            status = "ERROR";
                            title = "工具 \"" + row.get("display_name") + "\" 已禁用";
                            description = "工具 " + row.get("display_name") + " 已被禁用";
                        }
                    }
                    
                    return ActivityLog.builder()
                            .id(((Number) row.get("id")).longValue())
                            .activityType("TOOL_STATUS")
                            .title(title)
                            .description(description)
                            .status(status)
                            .timestamp(((java.sql.Timestamp) row.get("last_health_check")).toLocalDateTime())
                            .build();
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询工具活动失败", e);
            return new ArrayList<>();
        }
    }

    private List<ActivityLog> queryMcpActivities(LocalDateTime startTime, int limit) {
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT id, server_name, last_heartbeat, status FROM mcp_servers WHERE last_heartbeat >= ? AND is_delete = 0 ORDER BY last_heartbeat DESC LIMIT ?",
                startTime,
                limit
            );
            
            return results.stream()
                .map(row -> {
                    Integer status = (Integer) row.get("status");
                    String activityStatus = "SUCCESS";
                    String title = "MCP 服务器 \"" + row.get("server_name") + "\" 状态更新";
                    String description = "MCP 服务器 " + row.get("server_name") + " 运行正常";
                    
                    if (status != null && status == 0) {
                        activityStatus = "ERROR";
                        title = "MCP 服务器连接失败";
                        description = "MCP 服务器 " + row.get("server_name") + " 连接失败";
                    }
                    
                    return ActivityLog.builder()
                            .id(((Number) row.get("id")).longValue())
                            .activityType("MCP_STATUS")
                            .title(title)
                            .description(description)
                            .status(activityStatus)
                            .timestamp(((java.sql.Timestamp) row.get("last_heartbeat")).toLocalDateTime())
                            .build();
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询 MCP 活动失败", e);
            return new ArrayList<>();
        }
    }

    private List<ActivityLog> queryKbActivities(LocalDateTime startTime, int limit) {
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT id, doc_name, updated_at FROM knowledge_base_document WHERE updated_at >= ? AND is_delete = 0 ORDER BY updated_at DESC LIMIT ?",
                startTime,
                limit
            );
            
            return results.stream()
                .map(row -> ActivityLog.builder()
                        .id(((Number) row.get("id")).longValue())
                        .activityType("KB_UPDATE")
                        .title("知识库 \"" + row.get("doc_name") + "\" 更新完成")
                        .description("知识库文档 " + row.get("doc_name") + " 更新成功")
                        .status("SUCCESS")
                        .timestamp(((java.sql.Timestamp) row.get("updated_at")).toLocalDateTime())
                        .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询知识库活动失败", e);
            return new ArrayList<>();
        }
    }
    private boolean hasNotAdminAuthority(HttpSession session) {
        User loginUser = userService.getLoginUser(session);
        if (loginUser != null){
            return UserRoleEnum.ADMIN.getCode() != loginUser.getUserRole() && UserRoleEnum.SUPER_ADMIN.getCode() != loginUser.getUserRole();
        }
        return true;
    }
}
