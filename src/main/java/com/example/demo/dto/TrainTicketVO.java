package com.example.demo.dto;

import lombok.Data;

@Data
public class TrainTicketVO {
    private Long trainId;
    private String trainNumber;
    private String trainType;
    private String fromStation;
    private String toStation;
    private String startTime;
    private String endTime;
    private Integer duration;

    private Integer businessSeatCount;
    private Double businessSeatPrice;
    private Integer firstSeatCount;
    private Double firstSeatPrice;
    private Integer secondSeatCount;
    private Double secondSeatPrice;
    private Integer sleeperCount;
    private Double sleeperPrice;
}