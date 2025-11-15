package com.nushungry.reviewservice.enums;

public enum ReportReason {
    SPAM("垃圾信息"),
    OFFENSIVE("冒犯性内容"),
    FAKE("虚假评价"),
    OTHER("其他");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
