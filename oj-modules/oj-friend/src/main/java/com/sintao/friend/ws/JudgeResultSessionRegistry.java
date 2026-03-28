package com.sintao.friend.ws;

import com.alibaba.fastjson2.JSON;
import com.sintao.common.core.domain.dto.JudgeResultPushDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JudgeResultSessionRegistry {

    // 活跃的WebSocket会话
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 请求id到会话id的映射
    private final Map<String, Set<String>> requestSubscriptions = new ConcurrentHashMap<>();

    // 会话id到请求id的映射
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    public void remove(WebSocketSession session) {
        if (session == null) {
            return;
        }
        sessions.remove(session.getId());
        Set<String> requestIds = sessionSubscriptions.remove(session.getId());
        if (requestIds == null) {
            return;
        }
        for (String requestId : requestIds) {
            unsubscribe(session.getId(), requestId);
        }
    }

    public void subscribe(WebSocketSession session, String requestId) {
        register(session);
        requestSubscriptions.computeIfAbsent(requestId, key -> ConcurrentHashMap.newKeySet()).add(session.getId());
        sessionSubscriptions.computeIfAbsent(session.getId(), key -> ConcurrentHashMap.newKeySet()).add(requestId);
    }

    public void pushFinalResult(JudgeResultPushDTO dto) {
        if (dto == null || dto.getRequestId() == null) {
            return;
        }
        Set<String> sessionIds = requestSubscriptions.get(dto.getRequestId());
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        for (String sessionId : Set.copyOf(sessionIds)) {
            WebSocketSession session = sessions.get(sessionId);
            if (session == null || !session.isOpen()) {
                unsubscribe(sessionId, dto.getRequestId());
                continue;
            }
            Object sessionUserId = session.getAttributes().get(JudgeResultHandshakeInterceptor.ATTR_USER_ID);
            if (!(sessionUserId instanceof Long userId) || !userId.equals(dto.getUserId())) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(JSON.toJSONString(dto)));
            } catch (IOException ignored) {
                remove(session);
                continue;
            }
            unsubscribe(sessionId, dto.getRequestId());
        }
    }

    private void unsubscribe(String sessionId, String requestId) {
        Set<String> sessionIds = requestSubscriptions.get(requestId);
        if (sessionIds != null) {
            sessionIds.remove(sessionId);
            if (sessionIds.isEmpty()) {
                requestSubscriptions.remove(requestId);
            }
        }
        Set<String> requestIds = sessionSubscriptions.get(sessionId);
        if (requestIds != null) {
            requestIds.remove(requestId);
            if (requestIds.isEmpty()) {
                sessionSubscriptions.remove(sessionId);
            }
        }
    }
}
