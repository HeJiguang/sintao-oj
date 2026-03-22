package com.sintao.system.service.sysuser.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sintao.common.core.constants.Constants;
import com.sintao.common.core.constants.HttpConstants;
import com.sintao.common.core.domain.LoginUser;
import com.sintao.common.core.domain.R;
import com.sintao.common.core.domain.vo.LoginUserVO;
import com.sintao.common.core.enums.ResultCode;
import com.sintao.common.core.enums.UserIdentity;
import com.sintao.common.security.exception.ServiceException;
import com.sintao.common.security.service.TokenService;
import com.sintao.system.domain.sysuser.SysUser;
import com.sintao.system.domain.sysuser.dto.SysUserSaveDTO;
import com.sintao.system.mapper.sysuser.SysUserMapper;
import com.sintao.system.service.sysuser.ISysUserService;
import com.sintao.system.utils.BCryptUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RefreshScope
@Slf4j
public class SysUserServiceImpl implements ISysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private TokenService tokenService;

    @Value("${jwt.secret}")
    private String secret;

    @Override
    //维护性、性能、安�?    public R<String> login(String userAccount, String password) {
//        try {
//            FileInputStream inputStream = new FileInputStream("a.txt");
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//        int a = 100 / 0;
        //通过账号去数据库中查询，对应的用户信�?        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        //select password from tb_sys_user where user_account = #{userAccount}
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper
                .select(SysUser::getUserId, SysUser::getPassword, SysUser::getNickName)
                .eq(SysUser::getUserAccount, userAccount));
//        R loginResult = new R();
        if (sysUser == null) {
//            loginResult.setCode(ResultCode.FAILED_USER_NOT_EXISTS.getCode());
//            loginResult.setMsg(ResultCode.FAILED_USER_NOT_EXISTS.getMsg());
//            return loginResult;
            return R.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        if (BCryptUtils.matchesPassword(password, sysUser.getPassword())) {
//            loginResult.setCode(ResultCode.SUCCESS.getCode());
//            loginResult.setMsg(ResultCode.SUCCESS.getMsg());
//            return loginResult;
            //  jwttoken = 生产jwttoken的方�?            return R.ok(tokenService.createToken(sysUser.getUserId(),
                    secret, UserIdentity.ADMIN.getValue(), sysUser.getNickName(), null));
        }
//        loginResult.setCode(ResultCode.FAILED_LOGIN.getCode());
//        loginResult.setMsg(ResultCode.FAILED_LOGIN.getMsg());
//        return loginResult;
        return R.fail(ResultCode.FAILED_LOGIN);
    }

    @Override
    public boolean logout(String token) {
        if (StrUtil.isNotEmpty(token) && token.startsWith(HttpConstants.PREFIX)) {
            token = token.replaceFirst(HttpConstants.PREFIX, StrUtil.EMPTY);
        }
        return tokenService.deleteLoginUser(token, secret);
    }

    @Override
    public R<LoginUserVO> info(String token) {
        if (StrUtil.isNotEmpty(token) && token.startsWith(HttpConstants.PREFIX)) {
            token = token.replaceFirst(HttpConstants.PREFIX, StrUtil.EMPTY);
        }
        LoginUser loginUser = tokenService.getLoginUser(token, secret);
        if (loginUser == null) {
            return R.fail();
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setNickName(loginUser.getNickName());
        return R.ok(loginUserVO);
    }

    @Override
    public int add(SysUserSaveDTO sysUserSaveDTO) {
//        checkParams(sysUserSaveDTO);
        //重复
        //将dto转为实体
        List<SysUser> sysUserList = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserAccount, sysUserSaveDTO.getUserAccount()));
        //isNotEmpty  不为空返回true
//        if (sysUserList == null || sysUserList.size() == 0) {
//
//        }
        if (CollectionUtil.isNotEmpty(sysUserList)) {
            //用户已经存在
            //自定义的异常   公共的异常类
            throw new ServiceException(ResultCode.AILED_USER_EXISTS);
        }
        SysUser sysUser = new SysUser();
        sysUser.setUserAccount(sysUserSaveDTO.getUserAccount());
        sysUser.setPassword(BCryptUtils.encryptPassword(sysUserSaveDTO.getPassword()));
        sysUser.setCreateBy(Constants.SYSTEM_USER_ID);
        return sysUserMapper.insert(sysUser);
    }

//    private void checkParams(SysUserSaveDTO sysUserSaveDTO) {
//        String password = sysUserSaveDTO.getPassword();
//        if (password == null) {
//            throw new ServiceException();
//        }
//    }

    //编译时异�?
    //运行时异�?
    //全局异常处理    日志框架
    //1 增加全局异常处理

    // 2.当捕获到异常时，记录相关的日志，作为问题排查的线�?
}

