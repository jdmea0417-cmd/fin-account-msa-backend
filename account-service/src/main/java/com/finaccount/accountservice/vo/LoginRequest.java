package com.finaccount.accountservice.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotNull
    @Size(min = 12, max = 12)
    String accountNumber;

    @NotNull
    @Size(min = 8)
    String password;
}
