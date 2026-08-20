package com.esdllm.agentmesh.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理后台分页结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPageResult<T> {
    
    /**
     * 数据列表
     */
    private List<T> list;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页数量
     */
    private Integer pageSize;
    
    /**
     * 总页数
     */
    public Integer getTotalPages() {
        if (pageSize == null || pageSize <= 0) {
            return 0;
        }
        long totalPages = total / pageSize;
        if (total % pageSize > 0) {
            totalPages++;
        }
        return (int) totalPages;
    }
}
