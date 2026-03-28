package com.sintao.common.security.interceptor;

import cn.hutool.core.util.StrUtil;
import com.sintao.common.core.constants.Constants;
import com.sintao.common.core.constants.HttpConstants;
import com.sintao.common.core.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Token 拦截器（轻量版）
 * <p>
 * 不再自行解析 JWT 或访问 Redis，仅从网关透传的请求头中提取用户信息，
 * 并存入 ThreadLocal 供业务层使用。
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 直接从网关注入的内部请求头中获取用户信息
        String userIdStr = request.getHeader(HttpConstants.HEADER_USER_ID);
        String userKeyStr = request.getHeader(HttpConstants.HEADER_USER_KEY);

        if (StrUtil.isNotEmpty(userIdStr)) {
            ThreadLocalUtil.set(Constants.USER_ID, Long.valueOf(userIdStr));
            ThreadLocalUtil.set(Constants.USER_KEY, userKeyStr);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) {
        ThreadLocalUtil.remove();
    }
}
