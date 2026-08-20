package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.SysDict;
import com.esdllm.agentmesh.repository.dao.SysDictDao;
import com.esdllm.agentmesh.service.SysDictService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统字典服务实现类
 */
@Service
@Slf4j
public class SysDictServiceImpl implements SysDictService {

    @Resource
    private SysDictDao sysDictDao;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createDict(SysDict sysDict) {
        // 1. 基础参数校验
        validateBasicParams(sysDict);

        // 2. 检查字典类型和键是否已存在
        SysDict existingDict = sysDictDao.getByDictTypeAndKey(sysDict.getDictType(), sysDict.getDictKey());
        if (existingDict != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                "字典类型 [" + sysDict.getDictType() + "] 和键 [" + sysDict.getDictKey() + "] 已存在");
        }

        // 3. 设置默认值
        sysDict.setIsDelete(0);
        if (sysDict.getSortOrder() == null) {
            sysDict.setSortOrder(0);
        }

        // 4. 保存到数据库
        boolean saved = sysDictDao.save(sysDict);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建字典项失败");
        }

       log.info("创建字典项成功，dictId: {}, dictType: {}, dictKey: {}", 
            sysDict.getId(), sysDict.getDictType(), sysDict.getDictKey());
        return sysDict.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateDict(SysDict sysDict) {
        // 1. 基础参数校验
        validateBasicParams(sysDict);

        // 2. 查询字典项是否存在
        SysDict existingDict = sysDictDao.getById(sysDict.getId());
        if (existingDict == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "字典项不存在");
        }

        // 3. 不允许修改字典类型和键（保证唯一性）
        if (!existingDict.getDictType().equals(sysDict.getDictType()) || 
            !existingDict.getDictKey().equals(sysDict.getDictKey())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不允许修改字典类型和键");
        }

        // 4. 更新字典项
        boolean updated = sysDictDao.updateById(sysDict);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新字典项失败");
        }

       log.info("更新字典项成功，dictId: {}, dictType: {}, dictKey: {}", 
            sysDict.getId(), sysDict.getDictType(), sysDict.getDictKey());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteDict(Long dictId) {
        // 1. 查询字典项是否存在
        SysDict existingDict = sysDictDao.getById(dictId);
        if (existingDict == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "字典项不存在");
        }

        // 2. 使用 MyBatis-Plus 的逻辑删除
        boolean deleted = sysDictDao.removeById(dictId);
        if (!deleted) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除字典项失败");
        }

       log.info("删除字典项成功，dictId: {}", dictId);
        return true;
    }

    @Override
    public List<SysDict> getDictsByType(String dictType) {
        if (StrUtil.isBlank(dictType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典类型不能为空");
        }

        List<SysDict> dictList = sysDictDao.listByMap(
            java.util.Map.of("dict_type", dictType, "is_delete", 0)
        );
        
        return dictList;
    }

    @Override
    public SysDict getDictByKey(String dictType, Integer dictKey) {
        if (StrUtil.isBlank(dictType) || dictKey == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典类型和字典键不能为空");
        }

        SysDict sysDict = sysDictDao.getByDictTypeAndKey(dictType, dictKey);
        if (sysDict == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "字典项不存在");
        }

        return sysDict;
    }

    @Override
    public List<SysDict> getAllDicts() {
        List<SysDict> dictList = sysDictDao.listByMap(
            java.util.Map.of("is_delete", 0)
        );
        
        return dictList;
    }

    /**
     * 验证基础参数
     */
    private void validateBasicParams(SysDict sysDict) {
        // 字典类型不能为空
        if (StrUtil.isBlank(sysDict.getDictType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典类型不能为空");
        }

        // 字典类型格式校验（只允许字母、数字、下划线）
        if (!sysDict.getDictType().matches("^[a-zA-Z0-9_]+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典类型只能包含字母、数字和下划线");
        }

        // 字典键不能为空
        if (sysDict.getDictKey() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典键不能为空");
        }

        // 字典值不能为空
        if (StrUtil.isBlank(sysDict.getDictValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典值不能为空");
        }

        // 字典值长度限制
        if (sysDict.getDictValue().length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "字典值长度不能超过 100 个字符");
        }
    }
}
