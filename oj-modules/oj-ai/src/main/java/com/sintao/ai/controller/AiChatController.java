package com.sintao.ai.controller;

import com.sintao.ai.domain.dto.AiChatRequest;
import com.sintao.ai.service.AiChatService;
import com.sintao.common.core.controller.BaseController;
import com.sintao.common.core.domain.R;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 做题助手接口。
 * 网关 `/ai/**` 转发到本服务，`StripPrefix=1`，因此映射为 `/chat`。
 * 前端请求：`POST /dev-api/ai/chat`
 */
@RestController
@RequestMapping("/chat")
public class AiChatController extends BaseController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping
    public R<String> chat(@Valid @RequestBody AiChatRequest request) {
        try {
            String answer = aiChatService.chat(request);
            return R.ok(answer);
        } catch (Exception e) {
            // 返回具体异常信息便于排查，例如 API Key、base-url、网络超时等
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return R.fail(2000, "AI 调用失败：" + msg);
        }
    }
}

