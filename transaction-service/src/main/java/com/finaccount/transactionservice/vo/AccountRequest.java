package com.finaccount.transactionservice.vo;

import lombok.Data;

@Data
public class AccountRequest {
    private String accountNumber;

    private String ownerName;

    private Long balance;

    private AccountStatus status;
}
