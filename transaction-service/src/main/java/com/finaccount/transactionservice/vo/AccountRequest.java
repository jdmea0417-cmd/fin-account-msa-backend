package com.finaccount.transactionservice.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountRequest {
    private String accountNumber;

    private String ownerName;

    private Long balance;

    private AccountStatus status;
}
