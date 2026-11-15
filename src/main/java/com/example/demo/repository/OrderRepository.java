package com.example.demo.repository;

import com.example.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreateTimeDesc(Long userId);
    Optional<Order> findByOrderNo(String orderNo);

    List<Order> findByTrainIdAndTrainDate(Long trainId, String trainDate);

    List<Order> findByStatus(String status);

    long countByStatus(String status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.trainDate = :date AND o.status IN ('PAID','COMPLETED')")
    long countTodayPaid(String date);

    @Query("SELECT SUM(o.price) FROM Order o WHERE o.trainDate = :date AND o.status IN ('PAID','COMPLETED')")
    Double sumTodayRevenue(String date);

    @Query("SELECT o.fromStation, o.toStation, COUNT(o) as cnt FROM Order o " +
            "WHERE o.status IN ('PAID','COMPLETED') " +
            "GROUP BY o.fromStation, o.toStation ORDER BY cnt DESC")
    List<Object[]> findTopRoutes();
}