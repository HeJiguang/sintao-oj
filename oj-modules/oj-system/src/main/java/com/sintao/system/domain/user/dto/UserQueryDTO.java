package com.sintao.system.domain.user.dto;

import com.sintao.common.core.domain.PageQueryDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserQueryDTO extends PageQueryDTO {

    private Long userId;

    private String nickName;
}

