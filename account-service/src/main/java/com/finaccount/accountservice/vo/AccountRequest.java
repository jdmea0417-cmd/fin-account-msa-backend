package com.finaccount.accountservice.vo;

import com.finaccount.accountservice.dto.AccountStatus;
import lombok.Getter;

@Getter
public class AccountRequest {
    private String ownerName;

    private String password;

    private Long balance;

    private AccountStatus status;
}
