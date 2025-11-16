package com.nushungry.reviewservice.enums;

public enum ReportStatus {
    PENDING("待处理"),
    REVIEWING("处理中"),
    RESOLVED("已解决"),
    REJECTED("已驳回"),
    IGNORED("已忽略");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
