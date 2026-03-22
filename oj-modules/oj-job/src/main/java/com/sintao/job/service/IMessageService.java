package com.sintao.job.service;

import com.sintao.job.domain.message.Message;

import java.util.List;

public interface IMessageService {

    boolean batchInsert(List<Message> messageTextList);
}

