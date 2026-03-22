package com.sintao.common.core.enums;

public enum UserStatus {

    Block(0),

    Normal(1),
    ;


    private Integer value;

    UserStatus(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}

