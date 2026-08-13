package com.finaccount.transactionservice.jpa;

import com.finaccount.transactionservice.dto.TransactionStatus;
import com.finaccount.transactionservice.dto.TransactionType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Table
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer transactionId;

    @Column(nullable = true)
    private Integer fromAccountId;

    @Column(nullable = true)
    private Integer toAccountId;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private Instant createdAt;
}
