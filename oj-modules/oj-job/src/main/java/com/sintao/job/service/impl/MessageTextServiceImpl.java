package com.sintao.job.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sintao.job.domain.message.MessageText;
import com.sintao.job.mapper.message.MessageTextMapper;
import com.sintao.job.service.IMessageTextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MessageTextServiceImpl extends ServiceImpl<MessageTextMapper, MessageText> implements IMessageTextService {

    @Override
    public boolean batchInsert(List<MessageText> messageTextList) {
        return saveBatch(messageTextList);
    }
}

