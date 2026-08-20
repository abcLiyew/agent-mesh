package com.esdllm.agentmesh.repository.dao;

import com.esdllm.agentmesh.model.domain.SysDict;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author LiYehe
* @description 针对表【sys_dict】的数据库操作 Service
* @createDate 2026-03-09 13:26:59
*/
public interface SysDictDao extends IService<SysDict> {

    /**
     * 根据字典类型和键查询字典项
     * @param dictType 字典类型
     * @param dictKey 字典键
     * @return 字典项
     */
    SysDict getByDictTypeAndKey(String dictType, Integer dictKey);
}
