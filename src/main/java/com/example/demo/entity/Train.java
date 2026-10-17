package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "train")
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String trainNumber;

    @Column(nullable = false, length = 20)
    private String trainType;

    @Column(name = "from_station_id", nullable = false)
    private Long fromStationId;

    @Column(name = "to_station_id", nullable = false)
    private Long toStationId;

    @Column(nullable = false, length = 20)
    private String startTime;

    @Column(nullable = false, length = 20)
    private String endTime;

    @Column
    private Integer duration;

    @Column(name = "business_seat_price")
    private Double businessSeatPrice;

    @Column(name = "first_seat_price")
    private Double firstSeatPrice;

    @Column(name = "second_seat_price")
    private Double secondSeatPrice;

    @Column(name = "sleeper_price")
    private Double sleeperPrice;
}