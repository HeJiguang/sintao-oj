package com.sintao.common.security.exception;

import com.sintao.common.core.enums.ResultCode;

public class ServiceException extends RuntimeException {

    private final ResultCode resultCode;

    public ServiceException(ResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
