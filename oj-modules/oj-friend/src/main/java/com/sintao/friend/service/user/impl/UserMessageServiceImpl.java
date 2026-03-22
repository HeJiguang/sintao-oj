package com.sintao.friend.service.user.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.sintao.common.core.constants.Constants;
import com.sintao.common.core.domain.PageQueryDTO;
import com.sintao.common.core.domain.TableDataInfo;
import com.sintao.common.core.utils.ThreadLocalUtil;
import com.sintao.friend.domain.message.vo.MessageTextVO;
import com.sintao.friend.manager.MessageCacheManager;
import com.sintao.friend.mapper.message.MessageTextMapper;
import com.sintao.friend.service.user.IUserMessageService;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UserMessageServiceImpl implements IUserMessageService {

    @Autowired
    private MessageCacheManager messageCacheManager;

    @Autowired
    private MessageTextMapper messageTextMapper;

    @Override
    public TableDataInfo list(PageQueryDTO dto) {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        Long total = messageCacheManager.getListSize(userId);
        List<MessageTextVO> messageTextVOList;
        if (total == null || total <= 0) {
            //从数据库中查询我的竞赛列�?            PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
            messageTextVOList = messageTextMapper.selectUserMsgList(userId);
            messageCacheManager.refreshCache(userId);
            total = new PageInfo<>(messageTextVOList).getTotal();
        } else {
            messageTextVOList = messageCacheManager.getMsgTextVOList(dto, userId);
        }
        if (CollectionUtil.isEmpty(messageTextVOList)) {
            return TableDataInfo.empty();
        }
        return TableDataInfo.success(messageTextVOList, total);
    }
}

