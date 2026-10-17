package com.example.demo.entity;

public enum OrderStatus {
    PENDING,
    PAID,
    REFUNDED,
    CANCELLED,
    COMPLETED;

    public static OrderStatus from(String s) {
        if (s == null) return null;
        try {
            return OrderStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}