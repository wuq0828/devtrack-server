package com.nx.devtrack.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DevTrack 业务错误码,对齐 nx-skyline 的 Errors 枚举写法。
 * 约定:1xxx 认证/用户域,2xxx 缺陷域,9xxx 通用。
 */
@Getter
@AllArgsConstructor
public enum Errors implements IErrorEnum {

    NEED_LOGIN(1001, "未登录或登录已过期"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "用户名或密码错误"),
    NO_PERMISSION(1004, "无权限执行该操作"),
    FEISHU_AUTH_FAILED(1005, "飞书登录失败"),
    ROLE_REQUIRED(1006, "该操作需要指定角色"),

    DEFECT_NOT_FOUND(2001, "缺陷不存在"),
    ILLEGAL_TRANSITION(2002, "非法的状态流转"),
    TRANSITION_COMMENT_REQUIRED(2003, "该流转必须填写备注"),
    PROJECT_NOT_FOUND(2004, "项目不存在"),

    MIGRATION_FAILED(3001, "TAPD 数据迁移失败"),

    PARAM_INVALID(9001, "参数校验失败"),
    INTERNAL_ERROR(9999, "系统内部错误"),
    ;

    private final int code;
    private final String message;
}
