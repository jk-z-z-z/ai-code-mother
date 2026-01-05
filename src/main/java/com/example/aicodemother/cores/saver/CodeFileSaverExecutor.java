package com.example.aicodemother.cores.saver;

import com.example.aicodemother.ai.model.HtmlCodeResult;
import com.example.aicodemother.ai.model.MultiFileCodeResult;
import com.example.aicodemother.exception.BusinessException;
import com.example.aicodemother.exception.ErrorCode;
import com.example.aicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;

public class CodeFileSaverExecutor {
    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaverTemplate = new HtmlCodeFileSaverTemplate();
    private static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaverTemplate = new MultiFileCodeFileSaverTemplate();
    public static File executeSaver(Object result, CodeGenTypeEnum codeGenType,Long appId){
        return switch(codeGenType){
            case HTML -> htmlCodeFileSaverTemplate.saver((HtmlCodeResult) result,appId);
            case MULTI_FILE -> multiFileCodeFileSaverTemplate.saver((MultiFileCodeResult) result,appId);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR,"不存在类型");
        };
    }
}
