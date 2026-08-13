package com.finaccount.accountservice.dto;

import lombok.Getter;

@Getter
public class BalanceResponseDTO {
    private Integer accountId;

    private Long balance;
}
