package com.finaccount.transactionservice.dto;

import lombok.Getter;

@Getter
public class TransactionRequestDTO {
    private Integer fromAccountId;

    private Integer toAccountId;

    private Long amount;
}
