package com.example.demo.repository;

import com.example.demo.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByReceiverIdOrderByCreateTimeDesc(Long receiverId);

    List<Message> findBySenderIdAndReceiverIdOrderByCreateTimeAsc(Long senderId, Long receiverId);

    List<Message> findByReceiverIdAndIsReadFalse(Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiverId = :receiverId AND m.senderId = :senderId")
    int markRead(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    @Query("SELECT m FROM Message m WHERE (m.senderId = :uid OR m.receiverId = :uid) " +
           "AND m.createTime = (SELECT MAX(m2.createTime) FROM Message m2 " +
           "WHERE (m2.senderId = :uid OR m2.receiverId = :uid) " +
           "AND ((m2.senderId = m.senderId AND m2.receiverId = m.receiverId) " +
           "   OR (m2.senderId = m.receiverId AND m2.receiverId = m.senderId))) " +
           "ORDER BY m.createTime DESC")
    List<Message> findLatestPerConversation(@Param("uid") Long uid);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = :me AND m.senderId = :other AND m.isRead = false")
    long countUnreadFrom(@Param("me") Long me, @Param("other") Long other);
}