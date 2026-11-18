package com.example.demo.entity;

/** 发件箱事件状态：PENDING -> PROCESSING -> DONE / DEAD。 */
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    DONE,
    /** 重试耗尽后进入死信，等待人工/补偿任务介入。 */
    DEAD
}
