package com.nx.devtrack.app.controller;

import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import com.nx.devtrack.common.web.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理:统一转 CommonResponse,HTTP 状态码恒 200,业务结果看 code。
 * 对齐 nx-skyline:BizException -> fail(code, message)。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public CommonResponse<Void> handleBiz(BizException e) {
        return CommonResponse.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse(Errors.PARAM_INVALID.getMessage());
        return CommonResponse.fail(Errors.PARAM_INVALID.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public CommonResponse<Void> handleOther(Exception e) {
        log.error("unexpected error", e);
        return CommonResponse.fail(Errors.INTERNAL_ERROR);
    }
}
