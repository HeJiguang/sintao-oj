package com.sintao.friend.service.user;

import com.sintao.common.core.domain.R;
import com.sintao.common.core.domain.vo.LoginUserVO;
import com.sintao.friend.domain.user.dto.UserDTO;
import com.sintao.friend.domain.user.dto.UserUpdateDTO;
import com.sintao.friend.domain.user.vo.UserVO;

public interface IUserService {
    boolean sendCode(UserDTO userDTO);

    String codeLogin(String phone, String code);

    boolean logout(String token);

    R<LoginUserVO> info(String token);

    UserVO detail();

    int edit(UserUpdateDTO userUpdateDTO);

    int updateHeadImage(String headImage);
}

