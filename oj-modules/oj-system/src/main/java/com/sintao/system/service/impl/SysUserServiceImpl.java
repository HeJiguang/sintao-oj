package com.sintao.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sintao.common.core.domain.R;
import com.sintao.common.core.domain.enums.ResultCode;
import com.sintao.system.domain.SysUser;
import com.sintao.system.mapper.SysUserMapper;
import com.sintao.system.service.ISysUserService;
import com.sintao.system.utils.BCryptUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl implements ISysUserService {
    @Autowired
    private SysUserMapper sysUserMapper;


    @Override
    public R<Void> login(String userAccount, String password) {
        // 通过账号去数据库查询，对应的用户信息
        LambdaQueryWrapper<SysUser> qyeryWrapper = new LambdaQueryWrapper<>();
        // select password from tb_sys_user where user_account = #{userAccount}
        SysUser sysUser = sysUserMapper.selectOne(qyeryWrapper
                .select(SysUser::getPassword).eq(SysUser::getUserAccount, userAccount));

        R loginResult = new R();

        if(sysUser == null){
//            loginResult.setCode(ResultCode.FAILED_USER_NOT_EXISTS.getCode());
//            loginResult.setMsg(ResultCode.FAILED_USER_NOT_EXISTS.getMsg());
//            return loginResult;
            return R.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        if(BCryptUtils.matchesPassword(password, sysUser.getPassword())){
//            loginResult.setCode(ResultCode.SUCCESS.getCode());
//            loginResult.setMsg(ResultCode.SUCCESS.getMsg());
//            return  loginResult;
            return R.ok();
        }

//        loginResult.setCode(ResultCode.FAILED_LOGIN.getCode());
//        loginResult.setMsg(ResultCode.FAILED_LOGIN.getMsg());
//
//        return loginResult;
        return R.fail(ResultCode.FAILED_LOGIN);
    }
}
