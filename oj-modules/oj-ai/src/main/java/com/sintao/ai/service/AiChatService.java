package com.sintao.ai.service;

import com.sintao.ai.domain.dto.AiChatRequest;

/**
 * AI 做题助手：根据题目、代码、判题结果回答用户问题。
 */
public interface AiChatService {

    /**
     * 基于题目上下文与用户问题，调用大模型返回回答
     */
    String chat(AiChatRequest request);
}

