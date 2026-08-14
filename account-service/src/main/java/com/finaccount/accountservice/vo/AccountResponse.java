package com.finaccount.accountservice.vo;

import com.finaccount.accountservice.dto.AccountStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountResponse {
    private Integer accountNumber;

    private String ownerName;

    private Long balance;

    private AccountStatus status;
}
