package com.finaccount.transactionservice.vo;

import com.finaccount.transactionservice.dto.TransactionStatus;
import com.finaccount.transactionservice.dto.TransactionType;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;

@Data
public class TransactionResponse {
    private Integer transactionId;

    private Integer fromAccountId;

    private Integer toAccountId;

    private Long amount;

    private TransactionType type;

    private TransactionStatus status;

    private Instant createdAt;
}
