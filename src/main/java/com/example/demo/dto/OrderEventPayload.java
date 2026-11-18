package com.example.demo.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 下单事件负载：存进发件箱并投递到 MQ 的快照。 */
@Data
public class OrderEventPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String eventId;
    private Long orderId;
    private Long userId;
    private Long trainId;
    private String trainDate;
    private String seatType;
    private String passengerName;
    private String passengerIdCard;
}
