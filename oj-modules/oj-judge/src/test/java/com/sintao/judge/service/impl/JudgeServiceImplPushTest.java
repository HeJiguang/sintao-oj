package com.sintao.judge.service.impl;

import com.sintao.api.domain.dto.JudgeSubmitDTO;
import com.sintao.common.core.enums.CodeRunStatus;
import com.sintao.common.core.enums.JudgeAsyncStatus;
import com.sintao.common.redis.service.JudgeResultPushService;
import com.sintao.judge.domain.SandBoxExecuteResult;
import com.sintao.judge.mapper.UserSubmitMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JudgeServiceImplPushTest {

    @Test
    void requestBasedJudgeCompletionPublishesSuccessFinalResult() {
        JudgeServiceImpl judgeService = new JudgeServiceImpl();
        UserSubmitMapper userSubmitMapper = mock(UserSubmitMapper.class);
        SandboxPoolServiceImpl sandboxPoolService = mock(SandboxPoolServiceImpl.class);
        SandboxServiceImpl sandboxService = mock(SandboxServiceImpl.class);
        JudgeResultPushService judgeResultPushService = mock(JudgeResultPushService.class);
        ReflectionTestUtils.setField(judgeService, "userSubmitMapper", userSubmitMapper);
        ReflectionTestUtils.setField(judgeService, "sandboxPoolService", sandboxPoolService);
        ReflectionTestUtils.setField(judgeService, "sandboxService", sandboxService);
        ReflectionTestUtils.setField(judgeService, "judgeResultPushService", judgeResultPushService);

        JudgeSubmitDTO submitDTO = new JudgeSubmitDTO();
        submitDTO.setRequestId("req-1");
        submitDTO.setUserId(1001L);
        submitDTO.setDifficulty(1);
        submitDTO.setTimeLimit(1000L);
        submitDTO.setSpaceLimit(1024L);
        submitDTO.setInputList(List.of("1 2"));
        submitDTO.setOutputList(List.of("3"));
        when(sandboxPoolService.exeJavaCode(any(), any(), any()))
                .thenReturn(SandBoxExecuteResult.success(CodeRunStatus.SUCCEED, List.of("3"), 32L, 12L));

        judgeService.doJudgeJavaCode(submitDTO);

        verify(userSubmitMapper).update(any(), any());
        verify(judgeResultPushService).publishFinalResult(argThat(dto ->
                "req-1".equals(dto.getRequestId())
                        && Integer.valueOf(1001).equals(dto.getUserId().intValue())
                        && Integer.valueOf(JudgeAsyncStatus.SUCCESS.getValue()).equals(dto.getAsyncStatus())
                        && dto.getPass() != null
                        && dto.getExeMessage() != null));
    }
}
