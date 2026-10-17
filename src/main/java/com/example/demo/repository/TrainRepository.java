package com.example.demo.repository;

import com.example.demo.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrainRepository extends JpaRepository<Train, Long> {

    @Query("SELECT t FROM Train t WHERE t.fromStationId = :fromStationId AND t.toStationId = :toStationId")
    List<Train> findByStation(@Param("fromStationId") Long fromStationId,
                               @Param("toStationId") Long toStationId);

    Train findByTrainNumber(String trainNumber);

    boolean existsByTrainNumber(String trainNumber);
}