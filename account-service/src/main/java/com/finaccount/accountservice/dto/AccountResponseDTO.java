package com.finaccount.accountservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AccountResponseDTO {
    private Long accountNumber;

    private UUID ownerId;

    private Long balance;

    private AccountStatus status;
}
