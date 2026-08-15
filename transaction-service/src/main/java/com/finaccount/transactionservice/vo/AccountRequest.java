package com.finaccount.transactionservice.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountRequest {
    Integer accountId;

    Long balance;
}
