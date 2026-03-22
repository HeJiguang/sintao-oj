package com.sintao.system.service.user.impl;

import com.sintao.common.core.enums.ResultCode;
import com.sintao.common.security.exception.ServiceException;
import com.sintao.system.domain.user.User;
import com.sintao.system.domain.user.dto.UserDTO;
import com.sintao.system.domain.user.dto.UserQueryDTO;
import com.sintao.system.domain.user.vo.UserVO;
import com.sintao.system.manager.UserCacheManager;
import com.sintao.system.mapper.user.UserMapper;
import com.sintao.system.service.user.IUserService;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserCacheManager userCacheManager;

    @Override
    public List<UserVO> list(UserQueryDTO userQueryDTO) {
        PageHelper.startPage(userQueryDTO.getPageNum(), userQueryDTO.getPageSize());
        return userMapper.selectUserList(userQueryDTO);
    }

    @Override
    public int updateStatus(UserDTO userDTO) {
        User user = userMapper.selectById(userDTO.getUserId());
        if (user == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        user.setStatus(userDTO.getStatus());
        userCacheManager.updateStatus(user.getUserId(), userDTO.getStatus());
        return userMapper.updateById(user);
    }
}

