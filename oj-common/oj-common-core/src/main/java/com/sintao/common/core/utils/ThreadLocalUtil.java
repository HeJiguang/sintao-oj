package com.sintao.common.core.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadLocalUtil {
    private static final TransmittableThreadLocal<Map<String, Object>> THREAD_LOCAL = new TransmittableThreadLocal<>();

    public static void set(String key, Object value) {
        Map<String, Object> map = getLocalMap();
        map.put(key, value == null ? StrUtil.EMPTY : value);
    }

    public static <T> T get(String key, Class<T> clazz) {
        Map<String, Object> map = getLocalMap();
        return (T) map.getOrDefault(key, null);
    }

    public static Map<String, Object> getLocalMap() {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map == null) {
            map = new ConcurrentHashMap<String, Object>();
            THREAD_LOCAL.set(map);
        }
        return map;
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
