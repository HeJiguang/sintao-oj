package com.sintao.system.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 加密算法工具�? */
public class BCryptUtils {
    /**
     * 生成加密后密�?     *
     * @param password 密码
     * @return 加密字符�?     */
    public static String encryptPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    /**
     * 判断密码是否相同
     *
     * @param rawPassword     真实密码
     * @param encodedPassword 加密后密�?     * @return 结果
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public static void main(String[] args) {
        System.out.println(encryptPassword("123456"));
//        System.out.println(matchesPassword("123456", "$2a$10$S0hCQni3rpH/NObOlIFnWO9LOkyRgunMs34rX9UpGe2FENd1yke/m"));

    }
}

