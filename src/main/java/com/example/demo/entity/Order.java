package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "train_id", nullable = false)
    private Long trainId;

    @Column(nullable = false, length = 20)
    private String trainNumber;

    @Column(name = "from_station", length = 50)
    private String fromStation;

    @Column(name = "to_station", length = 50)
    private String toStation;

    @Column(length = 10)
    private String trainDate;

    @Column(length = 20)
    private String seatType;

    @Column(length = 20)
    private String seatNo;

    @Column(name = "passenger_name", length = 30)
    private String passengerName;

    @Column(name = "passenger_idcard", length = 20)
    private String passengerIdCard;

    @Column
    private Double price;

    @Column(length = 20)
    private String status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "pay_time")
    private LocalDateTime payTime;
}