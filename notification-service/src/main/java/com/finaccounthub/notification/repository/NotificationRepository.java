package com.finaccounthub.notification.repository;

import com.finaccounthub.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByOwnerNameOrderByReceivedAtDesc(String ownerName);

    List<NotificationEntity> findAllByOrderByReceivedAtDesc();

    boolean existsByTransactionId(Integer transactionId);
}
