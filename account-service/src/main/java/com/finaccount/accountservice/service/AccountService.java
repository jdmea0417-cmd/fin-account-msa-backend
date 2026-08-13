package com.finaccount.accountservice.service;

import com.finaccount.accountservice.dto.AccountRequestDTO;
import com.finaccount.accountservice.dto.AccountStatus;
import com.finaccount.accountservice.dto.BalanceResponseDTO;
import com.finaccount.accountservice.dto.AccountResponseDTO;
import com.finaccount.accountservice.jpa.AccountEntity;
import com.finaccount.accountservice.jpa.AccountRepository;
import com.finaccount.accountservice.jpa.BalanceEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponseDTO createAccount(AccountRequestDTO request) {
        AccountResponseDTO.AccountResponseDTOBuilder builder = AccountResponseDTO.builder();

        builder.ownerName(request.getOwnerName());
        builder.balance(0L);
        builder.status(AccountStatus.ACTIVE);

        return builder.build();
    }

    public AccountResponseDTO getAccount(Long accountId) {
        AccountEntity accountEntity = accountRepository.getAccountByAccountId(accountId);

        AccountResponseDTO accountResponseDTO = new ModelMapper().map(accountEntity, AccountResponseDTO.class);

        return accountResponseDTO;
    }

    public BalanceResponseDTO getBalance(Long accountId) {
         BalanceEntity balanceEntity = accountRepository.getBalanceByAccountId(accountId);

         BalanceResponseDTO balanceResponseDTO = new ModelMapper().map(balanceEntity, BalanceResponseDTO.class);

         return balanceResponseDTO;
    }
}
