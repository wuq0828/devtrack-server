package com.nx.devtrack.common.enums;

/**
 * 缺陷来源:手工录入 / 自动化测试 / TAPD 迁移。
 * AUTOMATION 来源的缺陷由 pytest 集成自动建单(见 integration-pytest)。
 */
public enum DefectSource {
    MANUAL,
    AUTOMATION,
    MIGRATION
}
