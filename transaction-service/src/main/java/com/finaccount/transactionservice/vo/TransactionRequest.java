package com.finaccount.transactionservice.vo;

import lombok.Getter;

@Getter
public class TransactionRequest {
    private Integer fromAccountId;

    private Integer toAccountId;

    private Long amount;
}
