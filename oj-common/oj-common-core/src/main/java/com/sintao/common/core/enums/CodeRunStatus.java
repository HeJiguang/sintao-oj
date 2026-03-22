package com.sintao.common.core.enums;

public enum CodeRunStatus {

    RUNNING(1, "运行中"),
    SUCCEED(2, "运行成功"),
    FAILED(3, "运行失败"),
    NOT_ALL_PASSED(4, "未通过所有用例"),
    UNKNOWN_FAILED(5, "未知异常，请您稍后重试"),
    COMPILE_FAILED(6, "编译失败"),
    OUT_OF_MEMORY(7, "运行结果正确，但是超出空间限制"),
    OUT_OF_TIME(8, "运行结果正确，但是超出时间限制");

    private final Integer value;
    private final String msg;

    CodeRunStatus(Integer value, String msg) {
        this.value = value;
        this.msg = msg;
    }

    public Integer getValue() {
        return value;
    }

    public String getMsg() {
        return msg;
    }
}
