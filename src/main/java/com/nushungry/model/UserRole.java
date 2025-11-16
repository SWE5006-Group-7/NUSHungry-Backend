package com.nushungry.model;

/**
 * 用户角色枚举
 * 定义系统中的用户角色类型
 */
public enum UserRole {
    /**
     * 普通用户角色
     */
    ROLE_USER("ROLE_USER", "普通用户"),

    /**
     * 管理员角色
     */
    ROLE_ADMIN("ROLE_ADMIN", "系统管理员");

    private final String value;
    private final String description;

    UserRole(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据字符串值获取枚举
     * @param value 角色字符串值
     * @return 对应的角色枚举，如果不存在返回null
     */
    public static UserRole fromValue(String value) {
        for (UserRole role : UserRole.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.value;
    }
}