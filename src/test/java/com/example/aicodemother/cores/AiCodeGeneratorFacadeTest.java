package com.example.aicodemother.cores;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.example.aicodemother.ai.AiCodeGeneratorService;
import com.example.aicodemother.ai.model.HtmlCodeResult;
import com.example.aicodemother.ai.model.MultiFileCodeResult;
import com.example.aicodemother.exception.BusinessException;
import com.example.aicodemother.exception.ErrorCode;
import com.example.aicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;


    @Test
    void generateAndSaveCode() {
        String userMessage="请帮我生成一个登陆界面，每部分不超过20行代码";
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.MULTI_FILE;
        File file=aiCodeGeneratorFacade.generateAndSaveCode(userMessage, codeGenTypeEnum);
        Assertions.assertNotNull(file);
        Assertions.assertTrue(file.exists());
    }

    @Test
    void generateAndSaveCodeStreaming() {
        String userMessage="请帮我生成一个登陆界面，每部分不超过20行代码";
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.MULTI_FILE;
        Flux<String> stringFlux = aiCodeGeneratorFacade.generateAndSaveCodeStreaming(userMessage, codeGenTypeEnum);
        Assertions.assertNotNull(stringFlux);
        List<String> result =stringFlux.collectList().block();
        Assertions.assertNotNull(result);
        String completeCode = String.join("\n", result);
        Assertions.assertFalse(completeCode.isEmpty());
    }
}