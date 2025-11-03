package com.sintao.system.service;

import com.sintao.common.core.domain.R;
import com.sintao.system.controller.LoginResult;
import org.springframework.stereotype.Service;

@Service
public interface ISysUserService {

    R<Void> login(String userAccount, String password);
}
