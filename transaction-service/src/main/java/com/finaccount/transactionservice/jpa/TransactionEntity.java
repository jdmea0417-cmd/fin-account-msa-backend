package com.finaccount.transactionservice.jpa;

import com.finaccount.transactionservice.dto.TransactionStatus;
import com.finaccount.transactionservice.dto.TransactionType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "transaction")
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
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(nullable = false)
    private Instant createdAt;
}

