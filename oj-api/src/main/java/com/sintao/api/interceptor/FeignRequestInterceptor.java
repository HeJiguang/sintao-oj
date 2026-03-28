package com.sintao.api.interceptor;

import cn.hutool.core.util.StrUtil;
import com.sintao.common.core.constants.HttpConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求前置拦截器：
 * 解决微服务之间内部调用时，导致请求上下文（如请求头中的用户身份信息）丢失的问题。
 * 作用是在发起 Feign 调用前，主动将原 HTTP 请求的 Header 转移到 Feign 的新请求中。
 */
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignRequestInterceptor.class);

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 提取当前进来的请求头中的核心鉴权信息
            String userId = request.getHeader(HttpConstants.HEADER_USER_ID);
            String userKey = request.getHeader(HttpConstants.HEADER_USER_KEY);
            String authorization = request.getHeader(HttpConstants.AUTHENTICATION);

            // 透传给由 Feign 发起的内部微服务调用目标
            if (StrUtil.isNotBlank(userId)) {
                template.header(HttpConstants.HEADER_USER_ID, userId);
            }
            if (StrUtil.isNotBlank(userKey)) {
                template.header(HttpConstants.HEADER_USER_KEY, userKey);
            }
            if (StrUtil.isNotBlank(authorization)) {
                template.header(HttpConstants.AUTHENTICATION, authorization);
            }
        }
    }
}
