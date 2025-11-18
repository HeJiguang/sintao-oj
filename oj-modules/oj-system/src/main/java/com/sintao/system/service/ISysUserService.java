package com.sintao.system.service;

import com.sintao.common.core.domain.domain.R;
import com.sintao.system.domain.SysUserSaveDTO;
import org.springframework.stereotype.Service;

@Service
public interface ISysUserService {

    R<String> login(String userAccount, String password);

    int add(SysUserSaveDTO sysUserSaveDTO);
}
