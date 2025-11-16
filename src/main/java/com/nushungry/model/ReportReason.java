package com.nushungry.model;

/**
 * 评价举报原因枚举
 */
public enum ReportReason {
    SPAM("垃圾信息", "包含广告、刷屏等垃圾内容"),
    OFFENSIVE("侮辱谩骂", "包含人身攻击、侮辱性语言"),
    INAPPROPRIATE("不当内容", "包含色情、暴力等不当内容"),
    FALSE_INFO("虚假信息", "提供虚假或误导性信息"),
    OFF_TOPIC("与摊位无关", "评价内容与摊位无关"),
    DUPLICATE("重复评价", "重复发布相同或类似评价"),
    OTHER("其他原因", "其他需要说明的原因");

    private final String displayName;
    private final String description;

    ReportReason(String displayName, String description) {
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
