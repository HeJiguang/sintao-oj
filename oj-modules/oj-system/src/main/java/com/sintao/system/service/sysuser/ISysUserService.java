package com.sintao.system.service.sysuser;

import com.sintao.common.core.domain.R;
import com.sintao.common.core.domain.vo.LoginUserVO;
import com.sintao.system.domain.sysuser.dto.SysUserSaveDTO;

public interface ISysUserService {
    R<String> login(String userAccount, String password);

    boolean logout(String token);

    R<LoginUserVO> info(String token);

    int add(SysUserSaveDTO sysUserSaveDTO);
}

