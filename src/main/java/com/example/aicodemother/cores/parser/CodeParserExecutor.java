package com.example.aicodemother.cores.parser;

import com.example.aicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;

public class CodeParserExecutor {

    private static final HtmlCodeParser htmlCodeParser= new HtmlCodeParser();

    private static final MultiFileCodeParser multiFileCodeParser= new MultiFileCodeParser();

    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case HTML -> htmlCodeParser.parse(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parse(codeContent);
            default -> throw new IllegalArgumentException("不支持的类型: " + codeGenType);
        };
    }
}
