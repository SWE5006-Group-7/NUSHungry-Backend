package com.nushungry.model;

/**
 * 审核状态枚举
 * PENDING - 待审核
 * APPROVED - 已通过
 * REJECTED - 已驳回
 */
public enum ModerationStatus {
    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String description;

    ModerationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
