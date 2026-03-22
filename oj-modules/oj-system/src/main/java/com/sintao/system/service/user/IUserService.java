package com.sintao.system.service.user;

import com.sintao.system.domain.user.dto.UserDTO;
import com.sintao.system.domain.user.dto.UserQueryDTO;
import com.sintao.system.domain.user.vo.UserVO;

import java.util.List;

public interface IUserService {

    List<UserVO> list(UserQueryDTO userQueryDTO);

    int updateStatus(UserDTO userDTO);
}

