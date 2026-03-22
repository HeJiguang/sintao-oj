package com.sintao.common.core.enums;

import lombok.Getter;

@Getter
public enum ProgramType {

    JAVA(0, "Java 语言"),

    CPP(1, "C++ 语言"),

    GOLANG(2, "Go 语言");

    private final Integer value;

    private final String desc;

    ProgramType(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
