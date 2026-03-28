package com.sintao.friend.rabbit;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sintao.common.core.enums.JudgeAsyncStatus;
import com.sintao.common.redis.service.JudgeResultPushService;
import com.sintao.common.redis.service.JudgeRuntimeStateService;
import com.sintao.friend.domain.user.UserSubmit;
import com.sintao.friend.mapper.user.UserSubmitMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JudgeProducerPushTest {

    @Test
    void dispatchFailurePublishesFinalResultEvent() {
        JudgeProducer producer = new JudgeProducer();
        UserSubmitMapper userSubmitMapper = mock(UserSubmitMapper.class);
        JudgeRuntimeStateService judgeRuntimeStateService = mock(JudgeRuntimeStateService.class);
        JudgeResultPushService judgeResultPushService = mock(JudgeResultPushService.class);
        UserSubmit userSubmit = new UserSubmit();
        userSubmit.setRequestId("req-1");
        userSubmit.setUserId(1001L);
        when(userSubmitMapper.selectOne(any())).thenReturn(userSubmit);
        ReflectionTestUtils.setField(producer, "userSubmitMapper", userSubmitMapper);
        ReflectionTestUtils.setField(producer, "judgeRuntimeStateService", judgeRuntimeStateService);
        ReflectionTestUtils.setField(producer, "judgeResultPushService", judgeResultPushService);

        ReflectionTestUtils.invokeMethod(producer, "markDispatchFailed", "req-1", "confirm failed");

        verify(userSubmitMapper).update(any(), any(UpdateWrapper.class));
        verify(judgeResultPushService).publishFinalResult(argThat(dto ->
                "req-1".equals(dto.getRequestId())
                        && Long.valueOf(1001L).equals(dto.getUserId())
                        && Integer.valueOf(JudgeAsyncStatus.DISPATCH_FAILED.getValue()).equals(dto.getAsyncStatus())
                        && "confirm failed".equals(dto.getLastError())));
    }
}
