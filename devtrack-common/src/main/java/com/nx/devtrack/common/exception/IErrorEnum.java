package com.nx.devtrack.common.exception;

/**
 * 错误码枚举约定,对齐 nx-common-core 的 IErrorEnum。
 */
public interface IErrorEnum {

    int getCode();

    String getMessage();
}
