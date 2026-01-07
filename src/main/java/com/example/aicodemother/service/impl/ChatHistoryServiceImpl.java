package com.example.aicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.aicodemother.exception.BusinessException;
import com.example.aicodemother.exception.ErrorCode;
import com.example.aicodemother.exception.ThrowUtils;
import com.example.aicodemother.mapper.ChatHistoryMapper;
import com.example.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.example.aicodemother.model.entity.App;
import com.example.aicodemother.model.entity.ChatHistory;
import com.example.aicodemother.model.entity.User;
import com.example.aicodemother.model.enums.UserRoleEnum;
import com.example.aicodemother.service.AppService;
import com.example.aicodemother.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;

    @Override
    public Page<ChatHistory> pageChatHistory(Long appId, int pageSize,
                                             LocalDateTime lastCreateTime,
                                             User loginuser){
        //参数校验
        ThrowUtils.throwIf(appId==null,ErrorCode.PARAMS_ERROR,"appId为空");
        ThrowUtils.throwIf(loginuser==null,ErrorCode.PARAMS_ERROR,"登陆用户信息为空");
        App app=appService.getById(appId);
        ThrowUtils.throwIf(app==null,ErrorCode.OPERATION_ERROR,"应用不存在");
        boolean isAdmin= loginuser.getUserRole().equals(UserRoleEnum.ADMIN.getValue());
        boolean isCreator=loginuser.getId().equals(app.getUserId());
        ThrowUtils.throwIf(!isAdmin&&!isCreator,ErrorCode.NO_AUTH_ERROR,"不是创建用户或管理员");

        ChatHistoryQueryRequest chatHistoryQueryRequest=new ChatHistoryQueryRequest();
        chatHistoryQueryRequest.setAppId(appId);
        chatHistoryQueryRequest.setLastCreateTime(lastCreateTime);

        QueryWrapper queryWrapper=this.getQueryWrapper(chatHistoryQueryRequest);
        return this.page(Page.of(1,pageSize),queryWrapper);
    }


    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        if (chatHistoryQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        Long id = chatHistoryQueryRequest.getId();
        String messageType = chatHistoryQueryRequest.getMessageType();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String messageContent = chatHistoryQueryRequest.getMessageContent();
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id",id)
                .eq("appId", appId)
                .eq("userId", userId)
                .eq("messageType", messageType)
                .like("message", messageContent)
                .lt("createTime", LocalDateTime.now());

        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

    @Override
    public Boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "appId不能为空");
        QueryWrapper queryWrapper=new QueryWrapper();
        queryWrapper.eq("appId",appId);
        return this.remove(queryWrapper);
    }
}
