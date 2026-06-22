package com.nx.devtrack.common.exception;

import lombok.Getter;

/**
 * 业务异常,对齐 nx-skyline 的 BizException:
 * 由全局异常处理器统一转成 CommonResponse.fail(code, message)。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(IErrorEnum error) {
        super(error.getMessage());
        this.code = error.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
