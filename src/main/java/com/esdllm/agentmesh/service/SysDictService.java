package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.model.domain.SysDict;

import java.util.List;

/**
 * 系统字典服务接口
 */
public interface SysDictService {
    
    /**
     * 创建字典项
     * @param sysDict 字典信息
     * @return 字典 ID
     */
    Long createDict(SysDict sysDict);
    
    /**
     * 更新字典项
     * @param sysDict 字典信息
     * @return 是否成功
     */
    Boolean updateDict(SysDict sysDict);
    
    /**
     * 删除字典项（逻辑删除）
     * @param dictId 字典 ID
     * @return 是否成功
     */
    Boolean deleteDict(Long dictId);
    
    /**
     * 根据字典类型获取字典列表
     * @param dictType 字典类型
     * @return 字典列表
     */
    List<SysDict> getDictsByType(String dictType);
    
    /**
     * 根据字典类型和键获取字典项
     * @param dictType 字典类型
     * @param dictKey 字典键
     * @return 字典项
     */
    SysDict getDictByKey(String dictType, Integer dictKey);
    
    /**
     * 获取所有字典
     * @return 字典列表
     */
    List<SysDict> getAllDicts();
}
