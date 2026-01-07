package com.example.aicodemother.controller;

import com.example.aicodemother.annotation.AuthCheck;
import com.example.aicodemother.common.BaseResponse;
import com.example.aicodemother.common.ResultUtils;
import com.example.aicodemother.constant.UserConstant;
import com.example.aicodemother.exception.BusinessException;
import com.example.aicodemother.exception.ErrorCode;
import com.example.aicodemother.exception.ThrowUtils;
import com.example.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.example.aicodemother.model.entity.App;
import com.example.aicodemother.model.entity.ChatHistory;
import com.example.aicodemother.model.entity.User;
import com.example.aicodemother.model.vo.ChatHistoryVO;
import com.example.aicodemother.service.AppService;
import com.example.aicodemother.service.ChatHistoryService;
import com.example.aicodemother.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private AppService appService;

    @Autowired
    private UserService userService;

    @PostMapping("/app/list/page")
    public BaseResponse<Page<ChatHistory>> listAppChatHistoryByPage(@RequestParam Long appId,
                                                                      @RequestParam int pageSize,
                                                                      @RequestParam LocalDateTime lastCreateTime,
                                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(appId==null,ErrorCode.PARAMS_ERROR);
        if(pageSize<=0){
            pageSize=10;
        }
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> chatHistoryPage = chatHistoryService.pageChatHistory(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(chatHistoryPage);
    }

    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistoryVO>> listChatHistoryByPage(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int pageNum = chatHistoryQueryRequest.getPageNum();
        int pageSize = chatHistoryQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageNum <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> chatHistoryPage = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        Page<ChatHistoryVO> voPage = new Page<>(pageNum, pageSize, chatHistoryPage.getTotalRow());
        List<ChatHistoryVO> voList = chatHistoryPage.getRecords().stream()
                .map(this::getChatHistoryVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }

    private ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory) {
        ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
        chatHistoryVO.setId(chatHistory.getId());
        chatHistoryVO.setMessage(chatHistory.getMessage());
        chatHistoryVO.setMessageType(chatHistory.getMessageType());
        chatHistoryVO.setAppId(chatHistory.getAppId());
        chatHistoryVO.setUserId(chatHistory.getUserId());
        chatHistoryVO.setParentId(chatHistory.getParentId());
        chatHistoryVO.setCreateTime(chatHistory.getCreateTime());
        return chatHistoryVO;
    }
}
