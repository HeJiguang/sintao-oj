package com.sintao.friend.ws;

import com.sintao.common.core.enums.JudgeAsyncStatus;
import com.sintao.common.core.domain.dto.JudgeResultPushDTO;
import com.sintao.common.redis.service.JudgeResultPushService;
import com.sintao.friend.domain.user.UserSubmit;
import com.sintao.friend.mapper.user.UserSubmitMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JudgeResultWebSocketHandlerTest {

    @Test
    void subscribeImmediatelyPushesCachedFinalResult() throws Exception {
        JudgeResultSessionRegistry registry = mock(JudgeResultSessionRegistry.class);
        JudgeResultPushService pushService = mock(JudgeResultPushService.class);
        UserSubmitMapper userSubmitMapper = mock(UserSubmitMapper.class);
        JudgeResultWebSocketHandler handler = new JudgeResultWebSocketHandler(registry, pushService, userSubmitMapper);
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(JudgeResultHandshakeInterceptor.ATTR_USER_ID, 1001L);
        when(session.getAttributes()).thenReturn(attributes);

        UserSubmit userSubmit = new UserSubmit();
        userSubmit.setRequestId("req-1");
        userSubmit.setUserId(1001L);
        when(userSubmitMapper.selectOne(any())).thenReturn(userSubmit);
        JudgeResultPushDTO dto = new JudgeResultPushDTO();
        dto.setRequestId("req-1");
        dto.setUserId(1001L);
        dto.setAsyncStatus(JudgeAsyncStatus.SUCCESS.getValue());
        dto.setFinishTime(LocalDateTime.now());
        when(pushService.getFinalResult("req-1")).thenReturn(dto);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"subscribe\",\"requestId\":\"req-1\"}"));

        verify(session).sendMessage(argThat(message ->
                message instanceof TextMessage textMessage
                        && textMessage.getPayload().contains("\"requestId\":\"req-1\"")));
        verify(registry, never()).subscribe(any(), anyString());
    }

    @Test
    void subscribeRejectsRequestIdThatDoesNotBelongToCurrentUser() throws Exception {
        JudgeResultSessionRegistry registry = mock(JudgeResultSessionRegistry.class);
        JudgeResultPushService pushService = mock(JudgeResultPushService.class);
        UserSubmitMapper userSubmitMapper = mock(UserSubmitMapper.class);
        JudgeResultWebSocketHandler handler = new JudgeResultWebSocketHandler(registry, pushService, userSubmitMapper);
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(JudgeResultHandshakeInterceptor.ATTR_USER_ID, 1001L);
        when(session.getAttributes()).thenReturn(attributes);
        when(userSubmitMapper.selectOne(any())).thenReturn(null);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"subscribe\",\"requestId\":\"other-user-req\"}"));

        verify(session).sendMessage(argThat(message ->
                message instanceof TextMessage textMessage
                        && textMessage.getPayload().contains("requestId")));
        verify(registry, never()).subscribe(any(), anyString());
    }
}
