package com.example.aicodemother.ai;

import com.example.aicodemother.service.ChatHistoryService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.Executors;
@Slf4j
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;
    @Resource
    private StreamingChatModel streamingChatModel;


    private final Cache<Long, AiCodeGeneratorService> aiServicesCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener(((key, value, cause) -> {
                log.info("AI实例移除，appId:{}，移除原因:{}", key, cause);
            }))
            .build();
    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;
    @Autowired
    private ChatHistoryService chatHistoryService;


    public AiCodeGeneratorService getOrCreateAiCodeGeneratorService(Long appId) {
        return aiServicesCache.get(appId, this::createAiCodeGeneratorService);
    }


    public AiCodeGeneratorService createAiCodeGeneratorService(Long appId) {
        log.info("创建AI实例，appId:{}", appId);
        MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
        chatHistoryService.loadChatHistoryToMemory(appId, messageWindowChatMemory, 10);
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(messageWindowChatMemory)
                .build();
    }
}
