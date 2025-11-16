package com.nushungry.model;

/**
 * 举报处理状态枚举
 */
public enum ReportStatus {
    PENDING("待处理", "举报已提交，等待管理员处理"),
    REVIEWING("处理中", "管理员正在审核"),
    RESOLVED("已处理", "举报已处理，采取了相应措施"),
    REJECTED("已驳回", "举报不成立或不需要处理"),
    CLOSED("已关闭", "举报已关闭");

    private final String displayName;
    private final String description;

    ReportStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
