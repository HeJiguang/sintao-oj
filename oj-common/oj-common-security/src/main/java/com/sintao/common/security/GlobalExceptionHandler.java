package com.sintao.common.security;

import com.sintao.common.core.domain.R;
import com.sintao.common.core.domain.enums.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
//import lombok.extern.slf4j.Slf4j;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 * 全局异常处理器
 */
@RestControllerAdvice
//@Slf4j
public class GlobalExceptionHandler
{
    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<?>
    handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                        HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
    // log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return R.fail(ResultCode.ERROR);
    }
    /**
     * 拦截运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public R<?> handleRuntimeException(RuntimeException e, HttpServletRequest
            request) {
        String requestURI = request.getRequestURI();
    // log.error("请求地址'{}',发生异常.", requestURI, e);
        return R.fail(ResultCode.ERROR);
    }
    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
    // log.error("请求地址'{}',发生异常.", requestURI, e);
        return R.fail(ResultCode.ERROR);

    }
}
