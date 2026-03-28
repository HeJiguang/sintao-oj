package com.sintao.friend.ws;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sintao.common.core.domain.dto.JudgeResultPushDTO;
import com.sintao.common.redis.service.JudgeResultPushService;
import com.sintao.friend.domain.user.UserSubmit;
import com.sintao.friend.mapper.user.UserSubmitMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JudgeResultWebSocketHandler extends TextWebSocketHandler {

    private final JudgeResultSessionRegistry registry;

    private final JudgeResultPushService pushService;

    private final UserSubmitMapper userSubmitMapper;

    public JudgeResultWebSocketHandler(JudgeResultSessionRegistry registry,
                                       JudgeResultPushService pushService,
                                       UserSubmitMapper userSubmitMapper) {
        this.registry = registry;
        this.pushService = pushService;
        this.userSubmitMapper = userSubmitMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        registry.register(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JudgeResultSubscribeMessage subscribeMessage;
        try {
            subscribeMessage = JSON.parseObject(message.getPayload(), JudgeResultSubscribeMessage.class);
        } catch (Exception ex) {
            sendError(session, null, "Invalid subscribe payload");
            return;
        }
        if (subscribeMessage == null
                || !"subscribe".equalsIgnoreCase(subscribeMessage.getType())
                || StrUtil.isBlank(subscribeMessage.getRequestId())) {
            sendError(session, null, "Invalid subscribe payload");
            return;
        }
        Object userIdAttr = session.getAttributes().get(JudgeResultHandshakeInterceptor.ATTR_USER_ID);
        if (!(userIdAttr instanceof Long userId)) {
            sendError(session, subscribeMessage.getRequestId(), "Unauthorized websocket session");
            return;
        }
        UserSubmit userSubmit = userSubmitMapper.selectOne(new QueryWrapper<UserSubmit>()
                .select("request_id", "user_id")
                .eq("request_id", subscribeMessage.getRequestId())
                .eq("user_id", userId));
        if (userSubmit == null) {
            sendError(session, subscribeMessage.getRequestId(), "requestId does not belong to current user");
            return;
        }
        JudgeResultPushDTO cached = pushService.getFinalResult(subscribeMessage.getRequestId());
        if (cached != null) {
            session.sendMessage(new TextMessage(JSON.toJSONString(cached)));
            return;
        }
        registry.subscribe(session, subscribeMessage.getRequestId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.remove(session);
    }

    private void sendError(WebSocketSession session, String requestId, String message) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "error");
        payload.put("requestId", requestId);
        payload.put("message", message);
        session.sendMessage(new TextMessage(JSON.toJSONString(payload)));
    }
}
