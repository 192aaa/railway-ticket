package com.example.demo.repository;

import com.example.demo.entity.TrainStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainStationRepository extends JpaRepository<TrainStation, Long> {
    List<TrainStation> findByTrainIdOrderBySort(Long trainId);
}