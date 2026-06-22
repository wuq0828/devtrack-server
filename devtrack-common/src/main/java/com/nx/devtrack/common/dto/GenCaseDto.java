package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI / 启发式生成的测试用例草稿。
 */
@Data
public class GenCaseDto implements Serializable {

    private String title;
    private String preconditions;
    private String steps;
    private String expected;
}
