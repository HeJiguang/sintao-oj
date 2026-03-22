package com.sintao.job.service;

import com.sintao.job.domain.message.MessageText;

import java.util.List;

public interface IMessageTextService {

    boolean batchInsert(List<MessageText> messageTextList);
}

