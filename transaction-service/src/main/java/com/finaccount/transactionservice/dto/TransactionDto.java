package com.finaccount.transactionservice.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class TransactionDto {
    private Integer transactionId;

    private Integer fromAccountId;

    private Integer toAccountId;

    private Long amount;

    private TransactionType type;

    private TransactionStatus status;

    private Instant createdAt;
}
