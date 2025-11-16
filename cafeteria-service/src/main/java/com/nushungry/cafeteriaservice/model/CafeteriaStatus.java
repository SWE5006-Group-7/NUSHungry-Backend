package com.nushungry.cafeteriaservice.model;

/**
 * 食堂营业状态枚举
 */
public enum CafeteriaStatus {
    /**
     * 营业中
     */
    OPEN("营业中"),

    /**
     * 已关闭
     */
    CLOSED("已关闭"),

    /**
     * 维护中
     */
    MAINTENANCE("维护中"),

    /**
     * 临时关闭
     */
    TEMPORARILY_CLOSED("临时关闭");

    private final String description;

    CafeteriaStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}