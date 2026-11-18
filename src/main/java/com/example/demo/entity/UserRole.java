package com.example.demo.entity;

/** 系统角色常量：普通用户与管理员。 */
public final class UserRole {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    private UserRole() {
    }

    public static boolean isValid(String role) {
        return USER.equals(role) || ADMIN.equals(role);
    }
}
