package com.example.demo.repository;

import com.example.demo.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTrainIdAndTrainDate(Long trainId, LocalDate trainDate);

    List<Ticket> findByTrainIdInAndTrainDate(List<Long> trainIds, LocalDate trainDate);

    long countByTrainDateBetween(LocalDate start, LocalDate end);

    @Modifying
    @Query(value = "UPDATE ticket SET business_seat_count = business_seat_count - 1 " +
            "WHERE train_id = :trainId AND train_date = :trainDate AND business_seat_count > 0",
            nativeQuery = true)
    int deductBusinessSeat(@Param("trainId") Long trainId, @Param("trainDate") LocalDate trainDate);

    @Modifying
    @Query(value = "UPDATE ticket SET first_seat_count = first_seat_count - 1 " +
            "WHERE train_id = :trainId AND train_date = :trainDate AND first_seat_count > 0",
            nativeQuery = true)
    int deductFirstSeat(@Param("trainId") Long trainId, @Param("trainDate") LocalDate trainDate);

    @Modifying
    @Query(value = "UPDATE ticket SET second_seat_count = second_seat_count - 1 " +
            "WHERE train_id = :trainId AND train_date = :trainDate AND second_seat_count > 0",
            nativeQuery = true)
    int deductSecondSeat(@Param("trainId") Long trainId, @Param("trainDate") LocalDate trainDate);

    @Modifying
    @Query(value = "UPDATE ticket SET sleeper_count = sleeper_count - 1 " +
            "WHERE train_id = :trainId AND train_date = :trainDate AND sleeper_count > 0",
            nativeQuery = true)
    int deductSleeper(@Param("trainId") Long trainId, @Param("trainDate") LocalDate trainDate);

    @Modifying
    @Query(value = "UPDATE ticket SET business_seat_count = business_seat_count + 1 " +
            "WHERE train_id = :trainId AND train_date = :trainDate",
            nativeQuery = true)
    int restoreBusinessSeat(@Param("trainId") Long trainId, @Param("trainDate") LocalDate trainDate);

    @Modifying
    @Query(value = "UPDATE ticket SET first_seat_count = first_seat_count + 1 " +
            "WHERE train_id = :trainId AND train_date = :trainDate",
            nativeQuery = true)
    int restoreFirstSeat(@Param("trainId") Long trainId, @Param("trainDate") LocalDate trainDate);

    @Modifying
    @Query(value = "UPDATE ticket SET second_seat_count = second_seat_count + 1 " +
            "WHERE train_id = :trainId AND train_date = :trainDate",
            nativeQuery = true)
    int restoreSecondSeat(@Param("trainId") Long trainId, @Param("trainDate") LocalDate trainDate);

    @Modifying
    @Query(value = "UPDATE ticket SET sleeper_count = sleeper_count + 1 " +
            "WHERE train_id = :trainId AND train_date = :trainDate",
            nativeQuery = true)
    int restoreSleeper(@Param("trainId") Long trainId, @Param("trainDate") LocalDate trainDate);
}