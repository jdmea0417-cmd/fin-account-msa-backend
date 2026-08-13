package com.finaccount.transactionservice.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class TransactionResponseDTO {
    private Integer transactionId;

    private Integer fromAccountId;

    private Integer toAccountId;

    private Long amount;

    private TransactionType type;

    private TransactionStatus status;

    private Instant createdAt;
}
