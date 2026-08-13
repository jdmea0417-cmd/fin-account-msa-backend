package com.finaccount.accountservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountResponseDTO {
    private Long accountNumber;

    private Integer ownerName;

    private Long balance;

    private AccountStatus status;
}
