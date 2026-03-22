package com.sintao.friend.manager;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sintao.common.core.constants.CacheConstants;
import com.sintao.common.redis.service.RedisService;
import com.sintao.friend.domain.user.User;
import com.sintao.friend.domain.user.vo.UserVO;
import com.sintao.friend.mapper.user.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class UserCacheManager {

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserMapper userMapper;

    public UserVO getUserById(Long userId) {
        String userKey = getUserKey(userId);
        UserVO userVO = redisService.getCacheObject(userKey, UserVO.class);
        if (userVO != null) {
            //将缓存延�?0min
            redisService.expire(userKey, CacheConstants.USER_EXP, TimeUnit.MINUTES);
            return userVO;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .select(User::getUserId,
                        User::getNickName,
                        User::getHeadImage,
                        User::getSex,
                        User::getEmail,
                        User::getPhone,
                        User::getWechat,
                        User::getIntroduce,
                        User::getSchoolName,
                        User::getMajorName,
                        User::getStatus)
                .eq(User::getUserId, userId));
        if (user == null) {
            return null;
        }
        refreshUser(user);
        userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    public void refreshUser(User user) {
        //刷新用户缓存
        String userKey = getUserKey(user.getUserId());
        redisService.setCacheObject(userKey, user);
        //设置用户缓存有效期为10分钟
        redisService.expire(userKey, CacheConstants.USER_EXP, TimeUnit.MINUTES);
    }

    //u:d:用户id
    private String getUserKey(Long userId) {
        return CacheConstants.USER_DETAIL + userId;
    }
}

