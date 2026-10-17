package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "ticket", uniqueConstraints = @UniqueConstraint(columnNames = {"train_id", "train_date"}))
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "train_id", nullable = false)
    private Long trainId;

    @Column(nullable = false)
    private LocalDate trainDate;

    @Column(name = "business_seat_count")
    private Integer businessSeatCount = 0;

    @Column(name = "first_seat_count")
    private Integer firstSeatCount = 0;

    @Column(name = "second_seat_count")
    private Integer secondSeatCount = 0;

    @Column(name = "sleeper_count")
    private Integer sleeperCount = 0;
}