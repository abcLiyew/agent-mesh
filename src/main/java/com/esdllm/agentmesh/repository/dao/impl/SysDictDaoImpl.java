package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.SysDict;
import com.esdllm.agentmesh.repository.dao.SysDictDao;
import com.esdllm.agentmesh.repository.mapper.SysDictMapper;
import org.springframework.stereotype.Service;

/**
* @author LiYehe
* @description 针对表【sys_dict】的数据库操作 Service 实现
* @createDate 2026-03-09 13:34:39
*/
@Service
public class SysDictDaoImpl extends ServiceImpl<SysDictMapper, SysDict>
    implements SysDictDao {

    @Override
    public SysDict getByDictTypeAndKey(String dictType, Integer dictKey) {
        LambdaQueryWrapper<SysDict> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysDict::getDictType, dictType)
                   .eq(SysDict::getDictKey, dictKey)
                   .eq(SysDict::getIsDelete, 0);
        return this.getOne(queryWrapper);
    }
}
