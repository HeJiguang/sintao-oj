package com.sintao.friend.service.user.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sintao.common.core.constants.CacheConstants;
import com.sintao.common.core.constants.Constants;
import com.sintao.common.core.constants.HttpConstants;
import com.sintao.common.core.domain.LoginUser;
import com.sintao.common.core.domain.R;
import com.sintao.common.core.domain.vo.LoginUserVO;
import com.sintao.common.core.enums.ResultCode;
import com.sintao.common.core.enums.UserIdentity;
import com.sintao.common.core.enums.UserStatus;
import com.sintao.common.core.utils.ThreadLocalUtil;
import com.sintao.common.message.service.AliSmsService;
import com.sintao.common.redis.service.RedisService;
import com.sintao.common.security.exception.ServiceException;
import com.sintao.common.security.service.TokenService;
import com.sintao.friend.domain.user.User;
import com.sintao.friend.domain.user.dto.UserDTO;
import com.sintao.friend.domain.user.dto.UserUpdateDTO;
import com.sintao.friend.domain.user.vo.UserVO;
import com.sintao.friend.manager.UserCacheManager;
import com.sintao.friend.mapper.user.UserMapper;
import com.sintao.friend.service.user.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AliSmsService aliSmsService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserCacheManager userCacheManager;

    @Value("${sms.code-expiration:5}")
    private Long phoneCodeExpiration;

    @Value("${sms.send-limit:3}")
    private Integer sendLimit;

    @Value("${sms.is-send:false}")
    private boolean isSend;  // 开关打开为 true，关闭为 false

    @Value("${jwt.secret}")
    private String secret;

    @Value("${file.oss.downloadUrl}")
    private String downloadUrl;

    @Override
    public boolean sendCode(UserDTO userDTO) {
        if (!checkPhone(userDTO.getPhone())) {
            throw new ServiceException(ResultCode.FAILED_USER_PHONE);
        }
        String phoneCodeKey = getPhoneCodeKey(userDTO.getPhone());
        Long expire = redisService.getExpire(phoneCodeKey, TimeUnit.SECONDS);
        if (expire != null && (phoneCodeExpiration * 60 - expire) < 60 ){
            throw new ServiceException(ResultCode.FAILED_FREQUENT);
        }
        // 统计当天验证码发送次数，超过限制时直接拒绝
        String codeTimeKey = getCodeTimeKey(userDTO.getPhone());
        Long sendTimes = redisService.getCacheObject(codeTimeKey, Long.class);
        if (sendTimes != null && sendTimes >= sendLimit) {
            throw new ServiceException(ResultCode.FAILED_TIME_LIMIT);
        }
        String code = isSend ? RandomUtil.randomNumbers(6) : Constants.DEFAULT_CODE;
        // 存储到 Redis，数据结构为 String，key 为 p:c:手机号，value 为验证码
        redisService.setCacheObject(phoneCodeKey, code, phoneCodeExpiration, TimeUnit.MINUTES);
        // 开发环境下在终端打印验证码，便于本地调试
        log.info("[验证码] 手机号 {}, 验证码 {}", userDTO.getPhone(), code);
        if (isSend) {
            boolean sendMobileCode = aliSmsService.sendMobileCode(userDTO.getPhone(), code);
            if (!sendMobileCode) {
                throw new ServiceException(ResultCode.FAILED_SEND_CODE);
            }
        }
        redisService.increment(codeTimeKey);
        if (sendTimes == null) {  // 说明是当天第一次发起获取验证码请求
                    LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
            redisService.expire(codeTimeKey, seconds, TimeUnit.SECONDS);
        }
        return true;
    }

    @Override
    public String codeLogin(String phone, String code) {
        checkCode(phone, code);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {  // 新用户，执行注册逻辑
            user = new User();
            user.setPhone(phone);
            user.setStatus(UserStatus.Normal.getValue());
            user.setCreateBy(Constants.SYSTEM_USER_ID);
            userMapper.insert(user);
        }
        return tokenService.createToken(user.getUserId(), secret, UserIdentity.ORDINARY.getValue(), user.getNickName(), user.getHeadImage());
//        if (user != null) {  // 说明是老用户
//            String cacheCode = redisService.getCacheObject(phoneCodeKey, String.class);
//            if (StrUtil.isEmpty(cacheCode)) {
//                throw new ServiceException(ResultCode.FAILED_INVALID_CODE);
//            }
//            if (!cacheCode.equals(code)) {
//                throw new ServiceException(ResultCode.FAILED_ERROR_CODE);
//            }
//            //楠岃瘉鐮佹瘮瀵规垚锟?//            redisService.deleteObject(phoneCodeKey);
//            return tokenService.createToken(user.getUserId(), secret, UserIdentity.ORDINARY.getValue(), user.getNickName());
//        }
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
        if (StrUtil.isNotEmpty(loginUser.getHeadImage())) {
            loginUserVO.setHeadImage(downloadUrl + loginUser.getHeadImage());
        }
        return R.ok(loginUserVO);
    }

    @Override
    public UserVO detail() {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        UserVO userVO = userCacheManager.getUserById(userId);
        if (userVO == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        if (StrUtil.isNotEmpty(userVO.getHeadImage())) {
            userVO.setHeadImage(downloadUrl + userVO.getHeadImage());
        }
        return userVO;
    }

    @Override
    public int edit(UserUpdateDTO userUpdateDTO) {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        user.setNickName(userUpdateDTO.getNickName());
        user.setSex(userUpdateDTO.getSex());
        user.setSchoolName(userUpdateDTO.getSchoolName());
        user.setMajorName(userUpdateDTO.getMajorName());
        user.setPhone(userUpdateDTO.getPhone());
        user.setEmail(userUpdateDTO.getEmail());
        user.setWechat(userUpdateDTO.getWechat());
        user.setIntroduce(userUpdateDTO.getIntroduce());
        // 刷新用户缓存
        userCacheManager.refreshUser(user);
        tokenService.refreshLoginUser(user.getNickName(),user.getHeadImage(),
                ThreadLocalUtil.get(Constants.USER_KEY, String.class));
        return userMapper.updateById(user);
    }

    @Override
    public int updateHeadImage(String headImage) {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        if (userId == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        user.setHeadImage(headImage);
        // 刷新用户缓存
        userCacheManager.refreshUser(user);
        tokenService.refreshLoginUser(user.getNickName(),user.getHeadImage(),
                ThreadLocalUtil.get(Constants.USER_KEY, String.class));
        return userMapper.updateById(user);
    }

    private void checkCode(String phone, String code) {
        String phoneCodeKey = getPhoneCodeKey(phone);
        String cacheCode = redisService.getCacheObject(phoneCodeKey, String.class);
        if (StrUtil.isEmpty(cacheCode)) {
            throw new ServiceException(ResultCode.FAILED_INVALID_CODE);
        }
        if (!cacheCode.equals(code)) {
            throw new ServiceException(ResultCode.FAILED_ERROR_CODE);
        }
        // 验证码比对成功后删除缓存
    }

    public static boolean checkPhone(String phone) {
        Pattern regex = Pattern.compile("^1[2|3|4|5|6|7|8|9][0-9]\\d{8}$");
        Matcher m = regex.matcher(phone);
        return m.matches();
    }

    private String getPhoneCodeKey(String phone) {
        return CacheConstants.PHONE_CODE_KEY + phone;
    }

    private String getCodeTimeKey(String phone) {
        return CacheConstants.CODE_TIME_KEY + phone;
    }
}

