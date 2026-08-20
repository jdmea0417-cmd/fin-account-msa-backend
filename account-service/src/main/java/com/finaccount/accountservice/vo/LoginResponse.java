package com.finaccount.accountservice.vo;

import lombok.Data;

@Data
public class LoginResponse {
    Integer accountId;

    String accessToken;
}
