package com.finaccounthub.notification.repository;

import com.finaccounthub.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByOwnerNameOrderByReceivedAtDesc(String ownerName);

    List<NotificationEntity> findAllByOrderByReceivedAtDesc();

    List<NotificationEntity> findByIsSuspiciousTrueOrderByReceivedAtDesc();

    boolean existsByTransactionId(Integer transactionId);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.ownerName = :ownerName AND n.receivedAt >= :since")
    long countRecentTransactionsByOwner(@Param("ownerName") String ownerName, @Param("since") LocalDateTime since);
}
