package com.sintao.ai.controller;

import com.sintao.ai.domain.dto.AiChatDetailResponse;
import com.sintao.ai.domain.dto.AiChatRequest;
import com.sintao.ai.service.ChatFacadeService;
import com.sintao.common.core.controller.BaseController;
import com.sintao.common.core.domain.R;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/chat")
public class AiChatController extends BaseController {

    @Autowired
    private ChatFacadeService chatFacadeService;

    @PostMapping
    public R<String> chat(@Valid @RequestBody AiChatRequest request) {
        try {
            return R.ok(chatFacadeService.chat(request));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return R.fail(2000, "AI chat failed: " + msg);
        }
    }

    @PostMapping("/detail")
    public R<AiChatDetailResponse> chatDetail(@Valid @RequestBody AiChatRequest request) {
        try {
            return R.ok(chatFacadeService.chatDetail(request));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return R.fail(2000, "AI chat detail failed: " + msg);
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(@Valid @RequestBody AiChatRequest request) {
        StreamingResponseBody body = outputStream -> chatFacadeService.streamChat(request)
                .doOnNext(chunk -> {
                    try {
                        outputStream.write(chunk.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to write stream response", e);
                    }
                })
                .blockLast();

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(body);
    }
}
