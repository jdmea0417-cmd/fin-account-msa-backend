package com.finaccount.accountservice.jpa;

import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<AccountEntity, Long> {
    AccountEntity getAccountByAccountId(Long accountId);

    BalanceEntity getBalanceByAccountId(Long accountId);
}
