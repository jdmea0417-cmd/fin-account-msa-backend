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
    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    public AccountResponseDTO createAccount(AccountRequestDTO request) {
        AccountResponseDTO.AccountResponseDTOBuilder builder = AccountResponseDTO.builder();

        builder.ownerName(request.getOwnerName());
        builder.balance(0L);
        builder.status(AccountStatus.ACTIVE);

        return builder.build();
    }

    public AccountResponseDTO getAccount(Long accountId) {
        AccountEntity entity = repository.getAccountByAccountId(accountId);

        AccountResponseDTO response = new ModelMapper().map(entity, AccountResponseDTO.class);

        return response;
    }

    public BalanceResponseDTO getBalance(Long accountId) {
         BalanceEntity entity = repository.getBalanceByAccountId(accountId);

         BalanceResponseDTO response = new ModelMapper().map(entity, BalanceResponseDTO.class);

         return response;
    }
}
