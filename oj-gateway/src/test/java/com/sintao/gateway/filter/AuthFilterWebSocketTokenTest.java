package com.sintao.gateway.filter;

import com.sintao.common.core.constants.HttpConstants;
import com.sintao.common.core.domain.LoginUser;
import com.sintao.common.core.enums.UserIdentity;
import com.sintao.common.core.utils.JwtUtils;
import com.sintao.common.redis.service.RedisService;
import com.sintao.gateway.properties.IgnoreWhiteProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthFilterWebSocketTokenTest {

    @Test
    public void websocketHandshakeCanReadTokenFromQueryWhenAuthorizationHeaderIsMissing() {
        AuthFilter filter = new AuthFilter();
        IgnoreWhiteProperties ignoreWhiteProperties = mock(IgnoreWhiteProperties.class);
        RedisService redisService = mock(RedisService.class);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(ignoreWhiteProperties.getWhites()).thenReturn(Collections.emptyList());
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(reactor.core.publisher.Mono.empty());
        ReflectionTestUtils.setField(filter, "ignoreWhite", ignoreWhiteProperties);
        ReflectionTestUtils.setField(filter, "redisService", redisService);
        ReflectionTestUtils.setField(filter, "secret", "test-secret");

        String token = createToken("1001", "uk-1");
        LoginUser loginUser = new LoginUser();
        loginUser.setIdentity(UserIdentity.ORDINARY.getValue());
        when(redisService.hasKey("logintoken:uk-1")).thenReturn(true);
        when(redisService.getCacheObject("logintoken:uk-1", LoginUser.class)).thenReturn(loginUser);

        MockServerHttpRequest request = MockServerHttpRequest.get("/friend/ws/judge/result")
                .queryParam("token", token)
                .header("Connection", "Upgrade")
                .header("Upgrade", "websocket")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(exchangeCaptor.capture());
        assertEquals("1001", exchangeCaptor.getValue().getRequest().getHeaders().getFirst(HttpConstants.HEADER_USER_ID));
        assertEquals("uk-1", exchangeCaptor.getValue().getRequest().getHeaders().getFirst(HttpConstants.HEADER_USER_KEY));
    }

    @Test
    public void nonWebSocketRequestDoesNotAcceptQueryTokenFallback() {
        AuthFilter filter = new AuthFilter();
        IgnoreWhiteProperties ignoreWhiteProperties = mock(IgnoreWhiteProperties.class);
        RedisService redisService = mock(RedisService.class);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(ignoreWhiteProperties.getWhites()).thenReturn(Collections.emptyList());
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(reactor.core.publisher.Mono.empty());
        ReflectionTestUtils.setField(filter, "ignoreWhite", ignoreWhiteProperties);
        ReflectionTestUtils.setField(filter, "redisService", redisService);
        ReflectionTestUtils.setField(filter, "secret", "test-secret");

        String token = createToken("1001", "uk-1");
        MockServerHttpRequest request = MockServerHttpRequest.get("/friend/question/detail")
                .queryParam("token", token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any(ServerWebExchange.class));
        assertNull(exchange.getResponse().getHeaders().getFirst(HttpConstants.HEADER_USER_ID));
    }

    private String createToken(String userId, String userKey) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userKey", userKey);
        return JwtUtils.createToken(claims, "test-secret");
    }
}
