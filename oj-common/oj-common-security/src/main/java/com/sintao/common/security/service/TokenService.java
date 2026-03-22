package com.sintao.common.security.service;

import cn.hutool.core.lang.UUID;
import com.sintao.common.core.constants.CacheConstants;
import com.sintao.common.core.constants.JwtConstants;
import com.sintao.common.core.domain.LoginUser;
import com.sintao.common.core.utils.JwtUtils;
import com.sintao.common.redis.service.RedisService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Token 相关服务：负责创建、解析、续期和删除登录态。
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    @Autowired
    private RedisService redisService;

    public String createToken(Long userId, String secret, Integer identity, String nickName, String headImage) {
        Map<String, Object> claims = new HashMap<>();
        String userKey = UUID.fastUUID().toString();
        claims.put(JwtConstants.LOGIN_USER_ID, userId);
        claims.put(JwtConstants.LOGIN_USER_KEY, userKey);
        String token = JwtUtils.createToken(claims, secret);

        String tokenKey = getTokenKey(userKey);
        LoginUser loginUser = new LoginUser();
        loginUser.setIdentity(identity);
        loginUser.setNickName(nickName);
        loginUser.setHeadImage(headImage);
        redisService.setCacheObject(tokenKey, loginUser, CacheConstants.EXP, TimeUnit.MINUTES);
        return token;
    }

    /**
     * 在身份认证通过之后调用，延长 Redis 中登录态的有效时间。
     */
    public void extendToken(Claims claims) {
        String userKey = getUserKey(claims);
        if (userKey == null) {
            return;
        }
        String tokenKey = getTokenKey(userKey);
        Long expire = redisService.getExpire(tokenKey, TimeUnit.MINUTES);
        if (expire != null && expire < CacheConstants.REFRESH_TIME) {
            redisService.expire(tokenKey, CacheConstants.EXP, TimeUnit.MINUTES);
        }
    }

    public LoginUser getLoginUser(String token, String secret) {
        String userKey = getUserKey(token, secret);
        if (userKey == null) {
            return null;
        }
        return redisService.getCacheObject(getTokenKey(userKey), LoginUser.class);
    }

    public boolean deleteLoginUser(String token, String secret) {
        String userKey = getUserKey(token, secret);
        if (userKey == null) {
            return false;
        }
        return redisService.deleteObject(getTokenKey(userKey));
    }

    public Long getUserId(Claims claims) {
        if (claims == null) {
            return null;
        }
        return Long.valueOf(JwtUtils.getUserId(claims));
    }

    public String getUserKey(Claims claims) {
        if (claims == null) {
            return null;
        }
        return JwtUtils.getUserKey(claims);
    }

    private String getUserKey(String token, String secret) {
        Claims claims = getClaims(token, secret);
        if (claims == null) {
            return null;
        }
        return JwtUtils.getUserKey(claims);
    }

    public Claims getClaims(String token, String secret) {
        try {
            Claims claims = JwtUtils.parseToken(token, secret);
            if (claims == null) {
                log.error("解析 token 出现异常，token={}", token);
                return null;
            }
            return claims;
        } catch (Exception e) {
            log.error("解析 token 出现异常，token={}", token, e);
            return null;
        }
    }

    public void refreshLoginUser(String nickName, String headImage, String userKey) {
        String tokenKey = getTokenKey(userKey);
        LoginUser loginUser = redisService.getCacheObject(tokenKey, LoginUser.class);
        if (loginUser == null) {
            return;
        }
        loginUser.setNickName(nickName);
        loginUser.setHeadImage(headImage);
        redisService.setCacheObject(tokenKey, loginUser);
    }

    private String getTokenKey(String userKey) {
        return CacheConstants.LOGIN_TOKEN_KEY + userKey;
    }
}
