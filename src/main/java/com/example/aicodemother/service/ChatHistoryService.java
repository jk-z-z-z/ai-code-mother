package com.example.aicodemother.service;

import com.example.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.example.aicodemother.model.entity.ChatHistory;
import com.example.aicodemother.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

public interface ChatHistoryService extends IService<ChatHistory> {

    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    Page<ChatHistory> pageChatHistory(Long appId, int pageSize,
                                      LocalDateTime lastCreateTime,
                                      User loginuser);

    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    Boolean deleteByAppId(Long appId);
}
