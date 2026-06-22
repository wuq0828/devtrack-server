package com.nx.devtrack.common.web;

import com.nx.devtrack.common.exception.IErrorEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回体,对齐 nx-skyline 的 CommonResponse 约定:
 * HTTP 状态码统一 200,业务结果通过 code 区分(0 = 成功)。
 *
 * 生产可替换为 nx-common-springboot-web 的 CommonResponse,删除本类即可。
 */
@Data
public class CommonResponse<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    public static <T> CommonResponse<T> ok(T data) {
        CommonResponse<T> r = new CommonResponse<>();
        r.code = 0;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static CommonResponse<Void> ok() {
        return ok(null);
    }

    public static <T> CommonResponse<T> fail(int code, String message) {
        CommonResponse<T> r = new CommonResponse<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> CommonResponse<T> fail(IErrorEnum error) {
        return fail(error.getCode(), error.getMessage());
    }
}
