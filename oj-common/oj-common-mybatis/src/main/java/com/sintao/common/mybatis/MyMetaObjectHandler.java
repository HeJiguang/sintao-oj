package com.sintao.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.sintao.common.core.constants.Constants;
import com.sintao.common.core.utils.ThreadLocalUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        //创建�? 获取当前用户用户id  如何获取当前调用接口的用户的id呢？
        this.strictInsertFill(metaObject, "createBy", Long.class, ThreadLocalUtil.get(Constants.USER_ID, Long.class));
    }

    @Override
    public void updateFill(MetaObject metaObject) {
//        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("updateBy", ThreadLocalUtil.get(Constants.USER_ID, Long.class), metaObject);
//        this.strictUpdateFill(metaObject, "updateBy", Long.class, ThreadLocalUtil.get(Constants.USER_ID, Long.class));
    }
}

