package com.sintao.common.redis.service;

import com.sintao.common.core.constants.CacheConstants;
import com.sintao.common.core.domain.dto.JudgeResultPushDTO;
import com.sintao.common.core.enums.JudgeAsyncStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JudgeResultPushServiceTest {

    @Test
    void publishFinalResultCachesAndPublishesRedisEvent() {
        RedisService redisService = mock(RedisService.class);
        RedisTemplate<Object, Object> redisTemplate = mock(RedisTemplate.class);
        JudgeResultPushService service = new JudgeResultPushService(redisService, redisTemplate);
        JudgeResultPushDTO dto = new JudgeResultPushDTO();
        dto.setRequestId("req-1");
        dto.setUserId(1001L);
        dto.setAsyncStatus(JudgeAsyncStatus.SUCCESS.getValue());
        dto.setFinishTime(LocalDateTime.now());

        service.publishFinalResult(dto);

        verify(redisService).setCacheObject(
                eq(CacheConstants.JUDGE_RESULT_CACHE + "req-1"),
                eq(dto),
                anyLong(),
                eq(TimeUnit.MINUTES)
        );
        verify(redisTemplate).convertAndSend(eq(CacheConstants.JUDGE_RESULT_TOPIC), anyString());
    }

    @Test
    void getFinalResultReadsFromRedisCache() {
        RedisService redisService = mock(RedisService.class);
        RedisTemplate<Object, Object> redisTemplate = mock(RedisTemplate.class);
        JudgeResultPushService service = new JudgeResultPushService(redisService, redisTemplate);
        JudgeResultPushDTO dto = new JudgeResultPushDTO();
        dto.setRequestId("req-2");
        when(redisService.getCacheObject(CacheConstants.JUDGE_RESULT_CACHE + "req-2", JudgeResultPushDTO.class))
                .thenReturn(dto);

        JudgeResultPushDTO actual = service.getFinalResult("req-2");

        assertSame(dto, actual);
    }
}
