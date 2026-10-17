package com.example.demo.entity;

public enum SeatType {

    BUSINESS_SEAT("商务座", "businessSeatCount", "business_seat_count", "businessSeatPrice"),
    FIRST_SEAT("一等座", "firstSeatCount", "first_seat_count", "firstSeatPrice"),
    SECOND_SEAT("二等座", "secondSeatCount", "second_seat_count", "secondSeatPrice"),
    SLEEPER("硬卧", "sleeperCount", "sleeper_count", "sleeperPrice");

    private final String label;
    private final String javaField;
    private final String dbColumn;
    private final String priceField;

    SeatType(String label, String javaField, String dbColumn, String priceField) {
        this.label = label;
        this.javaField = javaField;
        this.dbColumn = dbColumn;
        this.priceField = priceField;
    }

    public String getLabel() { return label; }
    public String getJavaField() { return javaField; }
    public String getDbColumn() { return dbColumn; }
    public String getPriceField() { return priceField; }

    public static SeatType fromLabel(String label) {
        for (SeatType type : values()) {
            if (type.label.equals(label)) return type;
        }
        throw new IllegalArgumentException("未知座位类型: " + label);
    }
}