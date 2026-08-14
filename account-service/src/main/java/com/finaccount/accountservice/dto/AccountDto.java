package com.finaccount.accountservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountDto {
    private Integer accountNumber;

    private String ownerName;

    private String password;

    private Long balance;

    private AccountStatus status;
}
