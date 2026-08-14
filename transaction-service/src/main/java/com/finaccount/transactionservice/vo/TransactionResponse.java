package com.finaccount.transactionservice.vo;

import com.finaccount.transactionservice.dto.TransactionStatus;
import com.finaccount.transactionservice.dto.TransactionType;
import lombok.Getter;

import java.time.Instant;

@Getter
public class TransactionResponse {
    private Integer transactionId;

    private Integer fromAccountId;

    private Integer toAccountId;

    private Long amount;

    private TransactionType type;

    private TransactionStatus status;

    private Instant createdAt;
}
