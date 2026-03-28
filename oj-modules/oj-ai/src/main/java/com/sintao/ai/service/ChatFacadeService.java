package com.sintao.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sintao.ai.client.PythonAgentClient;
import com.sintao.ai.config.AgentFacadeProperties;
import com.sintao.ai.domain.agent.AgentChatResponse;
import com.sintao.ai.domain.dto.AiChatDetailResponse;
import com.sintao.ai.domain.dto.AiChatRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatFacadeService implements AiChatService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ObjectProvider<AiChatService> legacyAiChatServiceProvider;

    private final PythonAgentClient pythonAgentClient;

    private final AgentFacadeProperties properties;

    public ChatFacadeService(
            @Qualifier("legacyAiChatService") ObjectProvider<AiChatService> legacyAiChatServiceProvider,
            PythonAgentClient pythonAgentClient,
            AgentFacadeProperties properties) {
        this.legacyAiChatServiceProvider = legacyAiChatServiceProvider;
        this.pythonAgentClient = pythonAgentClient;
        this.properties = properties;
    }

    @Override
    public String chat(AiChatRequest request) {
        String mode = properties.getMode();
        if (StringUtils.hasText(mode) && "langgraph".equalsIgnoreCase(mode.trim())) {
            return pythonAgentClient.chat(request);
        }
        return getLegacyAiChatService().chat(request);
    }

    public Flux<String> streamChat(AiChatRequest request) {
        String mode = properties.getMode();
        if (StringUtils.hasText(mode) && "langgraph".equalsIgnoreCase(mode.trim())) {
            return pythonAgentClient.streamChat(request);
        }

        String traceId = UUID.randomUUID().toString();
        String answer = getLegacyAiChatService().chat(request);
        return Flux.just(
                toSseEvent("meta", orderedPayload(
                        entry("trace_id", traceId),
                        entry("graph_version", "legacy"),
                        entry("mode", "legacy")
                )),
                toSseEvent("status", orderedPayload(
                        entry("node", "legacy"),
                        entry("message", "Using legacy Spring AI fallback.")
                )),
                toSseEvent("final", orderedPayload(
                        entry("answer", answer),
                        entry("confidence", 1.0),
                        entry("next_action", "Ask a more specific follow-up question if you need deeper guidance.")
                ))
        );
    }

    public AiChatDetailResponse chatDetail(AiChatRequest request) {
        String mode = properties.getMode();
        if (StringUtils.hasText(mode) && "langgraph".equalsIgnoreCase(mode.trim())) {
            AgentChatResponse response = pythonAgentClient.chatDetail(request);
            if (response == null || !StringUtils.hasText(response.getAnswer())) {
                throw new IllegalStateException("Python agent returned an empty answer");
            }
            AiChatDetailResponse detail = new AiChatDetailResponse();
            detail.setTraceId(response.getTraceId());
            detail.setMode("langgraph");
            detail.setIntent(response.getIntent());
            detail.setAnswer(response.getAnswer());
            detail.setConfidence(response.getConfidence() != null ? response.getConfidence() : 0.0);
            detail.setNextAction(response.getNextAction());
            return detail;
        }

        String answer = getLegacyAiChatService().chat(request);
        AiChatDetailResponse detail = new AiChatDetailResponse();
        detail.setTraceId(UUID.randomUUID().toString());
        detail.setMode("legacy");
        detail.setIntent("legacy_response");
        detail.setAnswer(answer);
        detail.setConfidence(1.0);
        detail.setNextAction("Ask a more specific follow-up question or switch to /chat/stream for status events.");
        return detail;
    }

    private AiChatService getLegacyAiChatService() {
        AiChatService legacyAiChatService = legacyAiChatServiceProvider.getIfAvailable();
        if (legacyAiChatService == null) {
            throw new IllegalStateException("Legacy AI chat service is unavailable");
        }
        return legacyAiChatService;
    }

    @SafeVarargs
    private static Map<String, Object> orderedPayload(Map.Entry<String, Object>... entries) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            payload.put(entry.getKey(), entry.getValue());
        }
        return payload;
    }

    private static Map.Entry<String, Object> entry(String key, Object value) {
        return Map.entry(key, value);
    }

    private String toSseEvent(String eventName, Map<String, Object> payload) {
        try {
            return "event: " + eventName + "\n"
                    + "data: " + OBJECT_MAPPER.writeValueAsString(payload) + "\n\n";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SSE payload", e);
        }
    }
}
