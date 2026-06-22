package com.nx.devtrack.common.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 请求 DTO 基类,对齐 nx-skyline-common 的 BaseInnerRequest。
 * 后续可承载通用字段(traceId、来源渠道等)。
 */
@Data
public class BaseInnerRequest implements Serializable {
}
