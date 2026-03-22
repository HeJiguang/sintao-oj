package com.sintao.judge.rabbit;

import com.sintao.api.domain.dto.JudgeSubmitDTO;
import com.sintao.common.core.constants.RabbitMQConstants;
import com.sintao.judge.service.IJudgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JudgeConsumer {

    @Autowired
    private IJudgeService judgeService;

    @RabbitListener(queues = RabbitMQConstants.OJ_WORK_QUEUE)
    public void consume(JudgeSubmitDTO judgeSubmitDTO) {
        log.info("收到消息：{}", judgeSubmitDTO);
        judgeService.doJudgeJavaCode(judgeSubmitDTO);
    }
}

