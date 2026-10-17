package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BuyTicketRequest {

    @NotNull(message = "车次ID不能为空")
    private Long trainId;

    @NotBlank(message = "日期不能为空")
    private String trainDate;

    @NotBlank(message = "座位类型不能为空")
    private String seatType;

    @NotBlank(message = "乘车人姓名不能为空")
    private String passengerName;

    @NotBlank(message = "乘车人证件号不能为空")
    private String passengerIdCard;
}