package com.sintao.friend.ws;

import cn.hutool.core.util.StrUtil;
import com.sintao.common.core.constants.HttpConstants;
import com.sintao.common.core.domain.LoginUser;
import com.sintao.common.core.enums.UserIdentity;
import com.sintao.common.security.service.TokenService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class JudgeResultHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "judgeWsUserId";

    public static final String ATTR_USER_KEY = "judgeWsUserKey";

    private final TokenService tokenService;

    @Value("${jwt.secret}")
    private String secret;

    public JudgeResultHandshakeInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = resolveToken(request); // 先从请求中找token
        if (StrUtil.isBlank(token)) {
            return false;
        }
        Claims claims = tokenService.getClaims(token, secret);
        if (claims == null) {
            return false;
        }
        LoginUser loginUser = tokenService.getLoginUser(token, secret);
        if (loginUser == null || !UserIdentity.ORDINARY.getValue().equals(loginUser.getIdentity())) {
            return false;
        }
        Long userId = tokenService.getUserId(claims);
        String userKey = tokenService.getUserKey(claims);
        if (userId == null || StrUtil.isBlank(userKey)) {
            return false;
        }
        attributes.put(ATTR_USER_ID, userId);
        attributes.put(ATTR_USER_KEY, userKey);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private String resolveToken(ServerHttpRequest request) {
        String headerToken = request.getHeaders().getFirst(HttpConstants.AUTHENTICATION);
        if (StrUtil.isNotBlank(headerToken)) {
            return stripBearer(headerToken);
        }
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        if (StrUtil.isNotBlank(token)) {
            return stripBearer(token);
        }
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return stripBearer(servletRequest.getServletRequest().getParameter("token"));
        }
        return null;
    }

    private String stripBearer(String token) {
        if (StrUtil.isNotBlank(token) && token.startsWith(HttpConstants.PREFIX)) {
            return token.replaceFirst(HttpConstants.PREFIX, StrUtil.EMPTY);
        }
        return token;
    }
}
