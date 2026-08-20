package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.KnowledgeBase;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.service.KnowledgeBaseService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/knowledge-base")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private UserService userService;

    @PostMapping("/add")
    public BaseResponse<Long> addKnowledgeBase(@RequestBody KnowledgeBase knowledgeBase, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long kbId = knowledgeBaseService.createKnowledgeBase(knowledgeBase, loginUser.getId());
        return ResultUtils.success(kbId);
    }

    @PutMapping("/update")
    public BaseResponse<Boolean> updateKnowledgeBase(@RequestBody KnowledgeBase knowledgeBase, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Boolean result = knowledgeBaseService.updateKnowledgeBase(knowledgeBase, loginUser.getId());
        return ResultUtils.success(result);
    }

    @DeleteMapping("/delete/{kbId}")
    public BaseResponse<Boolean> deleteKnowledgeBase(@PathVariable Long kbId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Boolean result = knowledgeBaseService.deleteKnowledgeBase(kbId, loginUser.getId());
        return ResultUtils.success(result);
    }

    @GetMapping("/my-list")
    public BaseResponse<List<KnowledgeBase>> getMyKnowledgeBases(
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        List<KnowledgeBase> kbList = knowledgeBaseService.getMyKnowledgeBases(loginUser.getId(), page, pageSize);
        return ResultUtils.success(kbList);
    }

    @GetMapping("/{kbId}")
    public BaseResponse<KnowledgeBase> getKnowledgeBase(@PathVariable String kbId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        KnowledgeBase kb = knowledgeBaseService.getKnowledgeBaseById(Long.valueOf(kbId), loginUser.getId());
        return ResultUtils.success(kb);
    }
}
