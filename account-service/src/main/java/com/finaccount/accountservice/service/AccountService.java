package com.finaccount.accountservice.service;

import com.finaccount.accountservice.dto.AccountStatus;
import com.finaccount.accountservice.dto.AccountDto;
import com.finaccount.accountservice.jpa.AccountEntity;
import com.finaccount.accountservice.jpa.AccountRepository;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AccountService implements UserDetailsService {
    private final AccountRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AccountService(AccountRepository repository, BCryptPasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public AccountDto createAccount(AccountDto dto) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        AccountEntity entity = mapper.map(dto, AccountEntity.class);
        entity.setAccountNumber(new AccountNumberGenerator().generate());
        entity.setBalance(0L);
        entity.setStatus(AccountStatus.ACTIVE);
        entity.setPassword(passwordEncoder.encode(dto.getPassword()));

        repository.save(entity);

        AccountDto created = mapper.map(entity, AccountDto.class);

        return created;
    }

    public AccountDto getAccountByAccountId(Integer accountId) throws NoSuchElementException {
        AccountEntity entity = repository.findById(accountId).orElseThrow();

        AccountDto dto = new ModelMapper().map(entity, AccountDto.class);

        return dto;
    }

    public AccountDto getAccountByAccountNumber(String accountNumber) throws NoSuchElementException {
        AccountEntity entity = repository.findByAccountNumber(accountNumber).orElseThrow();

        AccountDto dto = new ModelMapper().map(entity, AccountDto.class);

        return dto;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            AccountDto dto = getAccountByAccountNumber(username);

            return new User(
                    dto.getAccountNumber(),
                    dto.getPassword(),
                    true,
                    true,
                    true,
                    true,
                    List.of()
            );

        } catch (NoSuchElementException e) {
            throw new UsernameNotFoundException(username);
        }
    }

    public AccountDto updateAccount(Integer accountId, AccountDto dto) throws NoSuchElementException {
        AccountEntity entity = repository.findById(accountId).orElseThrow();

        if (isBalanceToBeUpdated(dto)) {
            entity.setBalance(dto.getBalance());
        }

        if (isStatusToBeUpdated(dto)) {
            entity.setStatus(dto.getStatus());
        }

        AccountEntity saved = repository.save(entity);

        AccountDto account = new ModelMapper().map(saved, AccountDto.class);

        return account;
    }

    private boolean isBalanceToBeUpdated(AccountDto dto) {
        return dto.getBalance() != null;
    }

    private boolean isStatusToBeUpdated(AccountDto dto) {
        return dto.getStatus() != null;
    }
}
