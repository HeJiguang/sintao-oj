package com.sintao.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "ai.agent.facade")
public class AgentFacadeProperties {

    private String mode = "legacy";

    private String baseUrl = "http://localhost:8000";

    private String chatPath = "/api/chat";

    private String streamPath = "/api/chat/stream";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getChatPath() {
        return chatPath;
    }

    public void setChatPath(String chatPath) {
        this.chatPath = chatPath;
    }

    public String getStreamPath() {
        return streamPath;
    }

    public void setStreamPath(String streamPath) {
        this.streamPath = streamPath;
    }

    public boolean isLanggraphMode() {
        return "langgraph".equalsIgnoreCase(mode);
    }

    public String getResolvedChatPath() {
        if (!StringUtils.hasText(chatPath)) {
            return "/api/chat";
        }
        return chatPath.startsWith("/") ? chatPath : "/" + chatPath;
    }

    public String getResolvedStreamPath() {
        if (!StringUtils.hasText(streamPath)) {
            return "/api/chat/stream";
        }
        return streamPath.startsWith("/") ? streamPath : "/" + streamPath;
    }
}
