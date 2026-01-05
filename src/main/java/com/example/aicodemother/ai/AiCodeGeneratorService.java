package com.example.aicodemother.ai;

import com.example.aicodemother.ai.model.HtmlCodeResult;
import com.example.aicodemother.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public interface AiCodeGeneratorService {
    /**
     *
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/code-gen-html-system-prompt.md")
    HtmlCodeResult generateHtmlCode(String userMessage);
    /**
     *
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/code-gen-multiFile-system-prompt.md")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     *
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/code-gen-html-system-prompt.md")
    Flux<String> generateHtmlCodeStreaming(String userMessage);
    /**
     *
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/code-gen-multiFile-system-prompt.md")
    Flux<String> generateMultiFileCodeStreaming(String userMessage);


}
