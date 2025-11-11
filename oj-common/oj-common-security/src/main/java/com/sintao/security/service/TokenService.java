package com.sintao.security.service;

import cn.hutool.core.lang.UUID;
import com.sintao.common.core.domain.constants.CacheCanstant;
import com.sintao.common.core.domain.constants.JwtConstands;
import com.sintao.common.redis.service.RedisService;
import com.sintao.common.core.domain.domain.LoginUser;
import com.sintao.common.core.domain.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// 操作用户登录token方法
@Service
@Slf4j
public class TokenService {

    @Autowired
    private RedisService redisService;

    public String createToken(Long userId, String secret, Integer identity) {
        // 生成唯一标识
        Map<String, Object> claims = new HashMap<>();
        String userKey = UUID.fastUUID().toString();

        claims.put(JwtConstands.LOGIN_USER_ID,userId);
        claims.put(JwtConstands.LOGIN_USER_KEY,userKey);
        String token = JwtUtils.createToken(claims, secret);



        String key = CacheCanstant.LOGIN_TOKEN_KEY +  userKey;

        LoginUser loginUser = new LoginUser();
        loginUser.setIdentity(identity);

        redisService.setCacheObject(key, loginUser, CacheCanstant.EXP, TimeUnit.MINUTES);

        return token;
    }

    // 延长 身份认证通过之后再使用
    public void extendToken(String token, String secret) {
        Claims claims = null;
        try{
            claims = JwtUtils.parseToken(token,secret);
            if(claims==null){
                log.error("解析token {} 出现异常", token);
                return;
            }
        }catch(Exception e){
            log.error("解析token {} 出现异常", token, e);
            return;
        }
        String userKey = JwtUtils.getUserKey(claims);
        String tokenKey = CacheCanstant.LOGIN_TOKEN_KEY +  userKey;

        Long expire = redisService.getExpire(tokenKey, TimeUnit.MINUTES);
        if(expire != null && expire < CacheCanstant.RESFESH_TIME){
            redisService.expire(tokenKey, CacheCanstant.EXP, TimeUnit.MINUTES);
        }

    }
}
