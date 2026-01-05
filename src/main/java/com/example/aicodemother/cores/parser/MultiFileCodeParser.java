package com.example.aicodemother.cores.parser;

import com.example.aicodemother.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

     @Override
    public MultiFileCodeResult parse(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        // 提取 HTML 代码
        String htmlCode = extractByPattern(HTML_CODE_PATTERN, codeContent);
        String cssCode = extractByPattern(CSS_CODE_PATTERN, codeContent);
        String jsCode = extractByPattern(JS_CODE_PATTERN, codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }
        return result;
    }

    private String extractByPattern(Pattern pattern, String codeContent) {
        Matcher matcher = pattern.matcher(codeContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @Deprecated
    private String extractJsCode(String codeContent) {
        Matcher matcher = JS_CODE_PATTERN.matcher(codeContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    @Deprecated
    private String extractCssCode(String codeContent) {
        Matcher matcher = CSS_CODE_PATTERN.matcher(codeContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    @Deprecated
    private static String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
