package com.sintao.security.interceptor;

import cn.hutool.core.util.StrUtil;
import com.sintao.common.core.domain.constants.HttpConstants;
import com.sintao.security.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = getToken(request);
        tokenService.extendToken(token, secret);
        return true;
    }

    private String getToken(HttpServletRequest request) {
        String token = request.getHeader(HttpConstants.AUTHENTICATION);
        if(StrUtil.isNotEmpty(token) && token.startsWith(HttpConstants.PREFIX)){
            token = token.replace(HttpConstants.PREFIX,"");
        }
        return token;
    }
}
