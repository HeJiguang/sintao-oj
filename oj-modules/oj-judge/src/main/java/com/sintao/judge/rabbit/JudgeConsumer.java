package com.sintao.judge.rabbit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rabbitmq.client.Channel;
import com.sintao.api.domain.dto.JudgeSubmitDTO;
import com.sintao.common.core.constants.RabbitMQConstants;
import com.sintao.common.core.domain.dto.JudgeResultPushDTO;
import com.sintao.common.core.enums.JudgeAsyncStatus;
import com.sintao.common.redis.service.JudgeResultPushService;
import com.sintao.common.redis.service.JudgeRuntimeStateService;
import com.sintao.judge.domain.UserSubmit;
import com.sintao.judge.mapper.UserSubmitMapper;
import com.sintao.judge.service.IJudgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JudgeConsumer {

    // 最大重试次数
    private static final int MAX_RETRY_TIMES = 3;

    // 分布式锁超时时间
    private static final long LOCK_TIMEOUT_SECONDS = 300;

    @Autowired
    private IJudgeService judgeService;

    @Autowired
    private UserSubmitMapper userSubmitMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JudgeRuntimeStateService judgeRuntimeStateService;

    @Autowired
    private JudgeResultPushService judgeResultPushService;

    @RabbitListener(queues = RabbitMQConstants.OJ_WORK_QUEUE, containerFactory = "manualAckRabbitListenerContainerFactory")
    public void consume(JudgeSubmitDTO judgeSubmitDTO, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag(); // 获取消息的唯一标识，用于后续的手动 ACK/NACK
        String requestId = judgeSubmitDTO.getRequestId();
        // 如果没有请求id，直接丢入死信队列
        if (requestId == null || requestId.isBlank()) {
            handleUnrecoverable(judgeSubmitDTO, deliveryTag, channel, 0, "Missing requestId");
            return;
        }

        // 从提交数据中拿到用户提交
        UserSubmit userSubmit = userSubmitMapper.selectOne(new LambdaQueryWrapper<UserSubmit>()
                .eq(UserSubmit::getRequestId, requestId));
        // 如果数据库中没有，则放入死信队列中
        if (userSubmit == null) {
            handleUnrecoverable(judgeSubmitDTO, deliveryTag, channel, currentRetryCount(message), "Missing user submit record");
            return;
        }

        // 幂等性校验  判断判题状态，是不是以及被处理过了，及以及到达了最终状态。
        Integer judgeStatus = userSubmit.getJudgeStatus();
        if (JudgeAsyncStatus.SUCCESS.getValue().equals(judgeStatus)
                || JudgeAsyncStatus.DEAD_LETTER.getValue().equals(judgeStatus)
                || JudgeAsyncStatus.DISPATCH_FAILED.getValue().equals(judgeStatus)) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        // 尝试获取这个消息的分布式锁，如果拿不到分布式锁，则说明这个消息是以及有人再处理的，则将重试次数加一，并且放入重试队列。
        boolean locked = judgeRuntimeStateService.tryLock(
                requestId,
                "judge-consumer-" + Thread.currentThread().getId(),
                LOCK_TIMEOUT_SECONDS
        );
        if (!locked) {
            handleRetry(judgeSubmitDTO, deliveryTag, channel, currentRetryCount(message) + 1, "Request already locked");
            return;
        }


        // 更新Redis状态，并且开始判题。
        try {
            judgeRuntimeStateService.markConsuming(requestId);
            log.info("Received judge message, requestId={}", requestId);
            judgeService.doJudgeJavaCode(judgeSubmitDTO);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 计算下一次重试次数
            int nextRetryCount = currentRetryCount(message) + 1;
            String lastError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            // 判断这个异常是可以重复的，并且小于最大可重复次数
            if (isRetryable(e) && nextRetryCount <= MAX_RETRY_TIMES) {
                handleRetry(judgeSubmitDTO, deliveryTag, channel, nextRetryCount, lastError);
            } else {
                handleUnrecoverable(judgeSubmitDTO, deliveryTag, channel, nextRetryCount, lastError);
            }
        } finally {
            judgeRuntimeStateService.unlock(requestId);
        }
    }


    // 放入死信交换机，并且更新状态，开始重试，并且手动确认ack
    private void handleRetry(JudgeSubmitDTO judgeSubmitDTO, long deliveryTag, Channel channel, int retryCount, String lastError) throws IOException {
        // 【第1步：尝试把消息转移到“重试专用队列”】
        if (publishToExchange(judgeSubmitDTO, RabbitMQConstants.OJ_JUDGE_DLX_EXCHANGE, RabbitMQConstants.JUDGE_RETRY_KEY, retryCount, lastError)) {

            // 【第2步：转移成功后，更新业务状态】
            judgeRuntimeStateService.markRetryWaiting(judgeSubmitDTO.getRequestId(), retryCount, lastError);

            // 【第3步：反常识的“确认签收”】
            channel.basicAck(deliveryTag, false);
            return;
        }

        // 【第4步：转移失败的终极退路】
        channel.basicNack(deliveryTag, false, true);
    }

    //  放入死信交换机，进入死信队列
    private void handleUnrecoverable(JudgeSubmitDTO judgeSubmitDTO,
                                     long deliveryTag,
                                     Channel channel,
                                     int retryCount,
                                     String lastError) throws IOException {
        if (publishToExchange(judgeSubmitDTO, RabbitMQConstants.OJ_JUDGE_DLX_EXCHANGE, RabbitMQConstants.JUDGE_DEAD_KEY, retryCount, lastError)) {
            markDeadLetter(judgeSubmitDTO.getRequestId(), retryCount, lastError);
            channel.basicAck(deliveryTag, false);
            return;
        }
        channel.basicNack(deliveryTag, false, true);
    }


    private boolean publishToExchange(JudgeSubmitDTO judgeSubmitDTO,
                                      String exchange,
                                      String routingKey,
                                      int retryCount,
                                      String lastError) {
        try {
            // 请求ID + 路由键 + 第几次重试 拼凑为唯一id
            CorrelationData correlationData = new CorrelationData(
                    judgeSubmitDTO.getRequestId() + "-" + routingKey + "-" + retryCount
            );
            rabbitTemplate.convertAndSend(exchange, routingKey, judgeSubmitDTO, message -> {
                message.getMessageProperties().setCorrelationId(judgeSubmitDTO.getRequestId());
                message.getMessageProperties().setMessageId(judgeSubmitDTO.getRequestId());
                message.getMessageProperties().setHeader("retryCount", retryCount);
                message.getMessageProperties().setHeader("lastError", lastError);
                return message;
            }, correlationData);
            // 在接受到Rabbit明确接受到message的情况下，才返回
            return correlationData.getFuture().get(5, TimeUnit.SECONDS).isAck();
        } catch (Exception e) {
            log.error("Failed to reroute judge message, requestId={}", judgeSubmitDTO.getRequestId(), e);
            return false;
        }
    }

    // 死信队列收尾工作，将Redis清理，给前端返回以及处理好
    private void markDeadLetter(String requestId, int retryCount, String lastError) {
        if (requestId == null || requestId.isBlank()) {
            return;
        }
        UserSubmit userSubmit = userSubmitMapper.selectOne(new QueryWrapper<UserSubmit>()
                .select("request_id", "user_id")
                .eq("request_id", requestId));
        LocalDateTime finishTime = LocalDateTime.now();
        judgeRuntimeStateService.markDeadLetter(requestId, retryCount, lastError);
        userSubmitMapper.update(null, new UpdateWrapper<UserSubmit>()
                .eq("request_id", requestId)
                .set("judge_status", JudgeAsyncStatus.DEAD_LETTER.getValue())
                .set("retry_count", retryCount)
                .set("last_error", lastError)
                .set("finish_time", finishTime));
        JudgeResultPushDTO pushDTO = new JudgeResultPushDTO();
        pushDTO.setRequestId(requestId);
        if (userSubmit != null) {
            pushDTO.setUserId(userSubmit.getUserId());
        }
        pushDTO.setAsyncStatus(JudgeAsyncStatus.DEAD_LETTER.getValue());
        pushDTO.setLastError(lastError);
        pushDTO.setFinishTime(finishTime);
        judgeResultPushService.publishFinalResult(pushDTO);
    }

    private int currentRetryCount(Message message) {
        Object retryCount = message.getMessageProperties().getHeaders().get("retryCount");
        if (retryCount instanceof Number number) {
            return number.intValue();
        }
        if (retryCount != null) {
            return Integer.parseInt(String.valueOf(retryCount));
        }
        return 0;
    }

    private boolean isRetryable(Exception e) {
        return !(e instanceof IllegalArgumentException);
    }
}
