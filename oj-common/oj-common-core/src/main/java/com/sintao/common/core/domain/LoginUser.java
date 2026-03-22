package com.sintao.common.core.domain;

public class LoginUser {

    private String nickName;   // 用户昵称
    private Integer identity;  // 1 表示普通用户，2 表示管理员用户
    private String headImage;  // 头像

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Integer getIdentity() {
        return identity;
    }

    public void setIdentity(Integer identity) {
        this.identity = identity;
    }

    public String getHeadImage() {
        return headImage;
    }

    public void setHeadImage(String headImage) {
        this.headImage = headImage;
    }
}
