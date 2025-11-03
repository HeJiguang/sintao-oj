package com.sintao.common.core.domain;

import com.sintao.common.core.domain.enums.ResultCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class R<T> {
    private int code; // 定义固定的code

    private String msg; // 通常是code的辅助说明，一个code对应一个msg

    private T data; // 请求某个接口返回的数据。

    public static <T> R<T> ok() {
        return assembleResult(null, ResultCode.SUCCESS);
    }

    public static <T> R<T> ok(T data) {
        return assembleResult(data, ResultCode.SUCCESS);
    }

    public static <T> R<T> fail() {
        return assembleResult(null, ResultCode.FAILED);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return assembleResult(null, resultCode);
    }


    private static <T> R<T> assembleResult(T data, ResultCode resultCode) {
        R<T> r = new R<T>();
        r.setCode(resultCode.getCode());
        r.setMsg(resultCode.getMsg());
        r.setData(data);
        return r;
    }
}
