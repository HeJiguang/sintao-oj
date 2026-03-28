package com.sintao.common.redis.service;

import com.alibaba.fastjson2.JSON;
import com.sintao.common.core.constants.CacheConstants;
import com.sintao.common.core.domain.dto.JudgeResultPushDTO;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

public class JudgeResultPushService {

    private static final long FINAL_RESULT_CACHE_TTL_MINUTES = 30L;

    private final RedisService redisService;

    private final RedisTemplate<Object, Object> redisTemplate;

    public JudgeResultPushService(RedisService redisService, RedisTemplate<Object, Object> redisTemplate) {
        this.redisService = redisService;
        this.redisTemplate = redisTemplate;
    }

    public void publishFinalResult(JudgeResultPushDTO dto) {
        if (dto == null || dto.getRequestId() == null || dto.getRequestId().isBlank()) {
            return;
        }
        String cacheKey = CacheConstants.JUDGE_RESULT_CACHE + dto.getRequestId();
        redisService.setCacheObject(cacheKey, dto, FINAL_RESULT_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.convertAndSend(CacheConstants.JUDGE_RESULT_TOPIC, JSON.toJSONString(dto));
    }

    public JudgeResultPushDTO getFinalResult(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        return redisService.getCacheObject(CacheConstants.JUDGE_RESULT_CACHE + requestId, JudgeResultPushDTO.class);
    }
}
