package com.sintao.common.core.domain.utils;

import com.sintao.common.core.domain.constants.JwtConstands;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.HashMap;
import java.util.Map;
public class JwtUtils {
    /**
     * 生成令牌
     *
     * @param claims 数据
     * @param secret 密钥
     * @return 令牌
     */
    public static String createToken(Map<String, Object> claims, String secret)
    {
        String token =
                Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512,
                        secret).compact();
        return token;
    }
    /**
     * 从令牌中获取数据
     *
     * @param token 令牌
     * @param secret 密钥
     * @return 数据
     */
    public static Claims parseToken(String token, String secret) {
        return
                Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    public static String getUserKey(Claims claims) {
        return toStr(claims.get(JwtConstands.LOGIN_USER_KEY));
    }

    public static String getUserId(Claims claims) {
        return toStr(claims.get(JwtConstands.LOGIN_USER_ID));
    }

    private static String toStr(Object value) {
        if(value==null) return "";
        return value.toString();
    }
}
