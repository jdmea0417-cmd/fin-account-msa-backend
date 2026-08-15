package com.finaccount.transactionservice.client;

import com.finaccount.transactionservice.vo.AccountRequest;
import com.finaccount.transactionservice.vo.AccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="account-service", configuration = FeignErrorDecoder.class)
public interface AccountServiceClient {
    @GetMapping("/accounts/{accountId}")
    AccountResponse getAccount(@PathVariable("accountId") Integer accountId);

    @PatchMapping("/accounts")
    AccountResponse patchAccount(@RequestBody AccountRequest request);
}
