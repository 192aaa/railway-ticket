package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QueryTicketRequest {

    @NotBlank(message = "出发站不能为空")
    private String fromStation;

    @NotBlank(message = "到达站不能为空")
    private String toStation;

    @NotBlank(message = "日期不能为空")
    private String date;
}