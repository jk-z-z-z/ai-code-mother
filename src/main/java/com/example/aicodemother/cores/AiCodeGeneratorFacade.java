package com.example.aicodemother.cores;

import com.example.aicodemother.ai.AiCodeGeneratorService;
import com.example.aicodemother.ai.model.HtmlCodeResult;
import com.example.aicodemother.ai.model.MultiFileCodeResult;
import com.example.aicodemother.cores.parser.CodeParserExecutor;
import com.example.aicodemother.cores.saver.CodeFileSaverExecutor;
import com.example.aicodemother.exception.BusinessException;
import com.example.aicodemother.exception.ErrorCode;
import com.example.aicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;



    private Flux<String> processCodeStreaming(Flux<String> code, CodeGenTypeEnum codeGenTypeEnum) {
        StringBuilder codeBuilder = new StringBuilder();
        return code
                .doOnNext(codeBuilder::append)
                .doOnComplete(() ->{
                    try {
                        String resultCode = codeBuilder.toString();
                        Object result = CodeParserExecutor.executeParser(resultCode, codeGenTypeEnum);
                        File file = CodeFileSaverExecutor.executeSaver(result, codeGenTypeEnum);
                        log.info("文件保存成功：{}", file.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("文件保存失败：{}", e.getMessage(), e);
                    }
                });
    }
    private File processCode(Object result, CodeGenTypeEnum codeGenTypeEnum) {
        return CodeFileSaverExecutor.executeSaver(result, codeGenTypeEnum);
    }

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield processCode(htmlCodeResult, codeGenTypeEnum);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield processCode(multiFileCodeResult, codeGenTypeEnum);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStreaming(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStreaming(userMessage);
                yield processCodeStreaming(result, codeGenTypeEnum);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStreaming(userMessage);
                yield processCodeStreaming(result, codeGenTypeEnum);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 生成 HTML 模式的代码并保存
     *
     * 该方法通过调用AI代码生成服务生成HTML代码，然后使用文件保存工具将结果保存到本地，用于指导AI生成相应的HTML代码
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveHtmlCode(String userMessage) {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML);
    }

    /**
     * 生成多文件模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveMultiFileCode(String userMessage) {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        return CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE);
    }

    /**
     * 生成 HTML 模式的代码并保存
     *
     * 该方法通过调用AI代码生成服务生成HTML代码，然后使用文件保存工具将结果保存到本地，用于指导AI生成相应的HTML代码
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    @Deprecated
    private Flux<String> generateAndSaveHtmlCodeStreaming(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStreaming(userMessage);
        StringBuilder codeBuilder = new StringBuilder();
        return result
                .doOnNext(codeBuilder::append)
                .doOnComplete(() ->{
                    try {
                        String code = codeBuilder.toString();
                        HtmlCodeResult htmlCodeResult= (HtmlCodeResult) CodeParserExecutor.executeParser(code, CodeGenTypeEnum.HTML);
                        File file = CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML);
                        log.info("文件保存成功：{}", file.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("文件保存失败：{}", e.getMessage(), e);
                    }
                });
    }

    /**
     * 生成多文件模式的代码并保存
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    @Deprecated
    private Flux<String> generateAndSaveMultiFileCodeStreaming(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStreaming(userMessage);
        StringBuilder codeBuilder = new StringBuilder();
        return result
                .doOnNext(code->{
                    codeBuilder.append(code);
                })
                .doOnComplete(() ->{
                    try {
                        String code = codeBuilder.toString();
                        MultiFileCodeResult multiFileCodeResult=CodeParser.parseMultiFileCode(code);
                        File file = CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
                        log.info("文件保存成功：{}", file.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("文件保存失败：{}", e.getMessage(), e);
                    }
                });
    }
}
