package com.nx.devtrack.common.web;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页包装器,对齐 nx-skyline 用 Page.map(...) 转 DTO 后返回前端的约定。
 */
@Data
public class Pagination<T> implements Serializable {

    private List<T> list;
    /** 当前页码,从 1 开始 */
    private int pn;
    /** 每页条数 */
    private int ps;
    private long total;

    public static <T> Pagination<T> of(List<T> list, int pn, int ps, long total) {
        Pagination<T> p = new Pagination<>();
        p.list = list;
        p.pn = pn;
        p.ps = ps;
        p.total = total;
        return p;
    }
}
