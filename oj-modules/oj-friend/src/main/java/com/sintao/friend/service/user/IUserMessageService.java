package com.sintao.friend.service.user;

import com.sintao.common.core.domain.PageQueryDTO;
import com.sintao.common.core.domain.TableDataInfo;

public interface IUserMessageService {
    TableDataInfo list(PageQueryDTO dto);
}

