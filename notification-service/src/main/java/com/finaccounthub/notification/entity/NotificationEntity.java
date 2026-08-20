package com.finaccounthub.notification.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 알림 로그 엔티티.
 * Kafka로부터 수신한 TransactionEvent를 가공하여 저장하며,
 * FDS(이상거래 탐지) 결과 필드를 포함한다.
 */
@Entity
@Table(name = "notification_log")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer transactionId;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String transactionType;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Boolean isSuspicious = false;

    @Column
    private Integer fromAccountId;

    @Column
    private Integer toAccountId;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    protected NotificationEntity() {
        // JPA
    }

    public NotificationEntity(Integer transactionId, String ownerName,
                               String transactionType, Long amount, String message,
                               String status, Boolean isSuspicious, Integer fromAccountId, Integer toAccountId,
                               LocalDateTime receivedAt) {
        this.transactionId = transactionId;
        this.ownerName = ownerName;
        this.transactionType = transactionType;
        this.amount = amount;
        this.message = message;
        this.status = status;
        this.isSuspicious = isSuspicious != null ? isSuspicious : false;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.receivedAt = receivedAt;
    }

    public Long getId() {
        return id;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public Long getAmount() {
        return amount;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getIsSuspicious() {
        return isSuspicious;
    }

    public Integer getFromAccountId() {
        return fromAccountId;
    }

    public Integer getToAccountId() {
        return toAccountId;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
}
