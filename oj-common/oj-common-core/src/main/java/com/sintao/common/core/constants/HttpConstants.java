package com.sintao.common.core.constants;

public class HttpConstants {
    /**
     * 服务端url标识
     */
    public static final String SYSTEM_URL_PREFIX = "system";

    /**
     * 用户端url标识
     */
    public static final String FRIEND_URL_PREFIX = "friend";

    /**
     * 令牌自定义标�?     */
    public static final String AUTHENTICATION = "Authorization";

    /**
     * 令牌前缀
     */
    public static final String PREFIX = "Bearer ";

    /**
     * 网关透传给下游微服务的内部请求头：用户ID
     */
    public static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 网关透传给下游微服务的内部请求头：用户Key（Redis会话标识）
     */
    public static final String HEADER_USER_KEY = "X-User-Key";
}

