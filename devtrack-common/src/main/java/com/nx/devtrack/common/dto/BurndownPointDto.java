package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BurndownPointDto implements Serializable {

    /** yyyy-MM-dd */
    private String date;

    /** 当天结束时仍未完成的缺陷数(实际) */
    private int remaining;

    /** 理想剩余(从 total 线性递减到 0) */
    private int idealRemaining;
}
