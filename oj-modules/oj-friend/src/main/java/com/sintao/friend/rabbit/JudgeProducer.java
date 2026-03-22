package com.sintao.friend.rabbit;

import com.sintao.api.domain.dto.JudgeSubmitDTO;
import com.sintao.common.core.constants.RabbitMQConstants;
import com.sintao.common.core.enums.ResultCode;
import com.sintao.common.security.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JudgeProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void produceMsg(JudgeSubmitDTO judgeSubmitDTO) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConstants.OJ_WORK_QUEUE, judgeSubmitDTO);
        } catch (Exception e) {
            log.error("生产者发送消息异�?, e);
            throw new ServiceException(ResultCode.FAILED_RABBIT_PRODUCE);
        }
    }
}

