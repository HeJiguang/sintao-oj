package com.sintao.common.core.enums;

import lombok.Getter;

@Getter
public enum QuestionResType {

    ERROR(0), //未通过

    PASS(1), //通过

    UN_SUBMIT(2),  //未提�?
    IN_JUDGE(3); //  系统判题�?
    private Integer value;

    QuestionResType(Integer value) {
        this.value = value;
    }
}

