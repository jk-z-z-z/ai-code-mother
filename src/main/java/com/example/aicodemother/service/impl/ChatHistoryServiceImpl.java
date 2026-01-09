package com.example.aicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.example.aicodemother.exception.BusinessException;
import com.example.aicodemother.exception.ErrorCode;
import com.example.aicodemother.exception.ThrowUtils;
import com.example.aicodemother.mapper.ChatHistoryMapper;
import com.example.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.example.aicodemother.model.entity.App;
import com.example.aicodemother.model.entity.ChatHistory;
import com.example.aicodemother.model.entity.User;
import com.example.aicodemother.model.enums.MessageTypeEnum;
import com.example.aicodemother.model.enums.UserRoleEnum;
import com.example.aicodemother.service.AppService;
import com.example.aicodemother.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;

/**
 * 加载聊天历史到内存中的方法
 * @param appId 应用ID，用于标识特定的聊天应用
 * @param chatMemory 聊天记忆窗口对象，用于存储历史消息
 * @param maxCount 最大加载的历史消息数量
 * @return 实际加载的消息数量，如果发生异常则返回0
 */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)  // 设置应用ID过滤条件
                    .orderBy(ChatHistory::getCreateTime, false)  // 按创建时间倒序排列
                    .limit(1, maxCount);  // 设置查询范围，从第1条开始，查询maxCount条记录
            List<ChatHistory> historyList = this.list(queryWrapper);  // 执行查询获取历史记录列表
            if (CollUtil.isEmpty(historyList)) {  // 检查列表是否为空
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {  // 遍历历史记录列表
                if (MessageTypeEnum.USER.getValue().equals(history.getMessageType())) {  // 判断是否为用户消息
                    chatMemory.add(UserMessage.from(history.getMessage()));  // 添加用户消息到记忆中
                    loadedCount++;
                } else if (MessageTypeEnum.AI.getValue().equals(history.getMessageType())) {  // 判断是否为AI消息
                    chatMemory.add(AiMessage.from(history.getMessage()));  // 添加AI消息到记忆中
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);  // 记录成功加载日志
            return loadedCount;
        } catch (Exception e) {  // 捕获所有异常
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);  // 记录错误日志
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }


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
