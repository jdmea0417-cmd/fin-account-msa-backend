package com.finaccount.transactionservice.service;

import com.finaccount.transactionservice.TransactionEvent;
import com.finaccount.transactionservice.client.AccountServiceClient;
import com.finaccount.transactionservice.dto.TransactionDto;
import com.finaccount.transactionservice.dto.TransactionStatus;
import com.finaccount.transactionservice.jpa.TransactionEntity;
import com.finaccount.transactionservice.jpa.TransactionRepository;
import com.finaccount.transactionservice.vo.AccountRequest;
import com.finaccount.transactionservice.vo.AccountResponse;
import com.finaccount.transactionservice.vo.AccountStatus;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final String TOPIC = "fin.transaction.events";

    private final TransactionRepository repository;
    private final AccountServiceClient accountService;
    private final KafkaProducer<String, TransactionEvent> kafkaProducer;
    private final CircuitBreakerFactory circuitBreakerFactory;

    public TransactionService(
            TransactionRepository repository,
            AccountServiceClient accountService,
            KafkaProducer<String, TransactionEvent> kafkaProducer,
            CircuitBreakerFactory circuitBreakerFactory
    ) {
        this.repository = repository;
        this.accountService = accountService;
        this.kafkaProducer = kafkaProducer;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    @Transactional
    public TransactionDto addTransaction(TransactionDto dto) {
        if (dto.getAmount() == null || dto.getAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction amount must be greater than 0");
        }

        TransactionDto pending = addPendingTransaction(dto);

        TransactionDto finalized;
        try {
            updateAccount(pending);
            finalized = updateToSuccessTransaction(pending);
        } catch (Exception e) {
            log.error("Transaction processing failed: {}. Marking transaction as FAILED.", e.getMessage(), e);
            finalized = updateToFailedTransaction(pending);
            sendNotification(finalized);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction failed: " + e.getMessage());
        }

        sendNotification(finalized);

        return finalized;
    }

    private TransactionDto addPendingTransaction(TransactionDto dto) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        TransactionEntity entity = mapper.map(dto, TransactionEntity.class);
        entity.setType(dto.getType());
        entity.setStatus(TransactionStatus.PENDING);
        entity.setCreatedAt(Instant.now());
        entity = repository.save(entity);

        return mapper.map(entity, TransactionDto.class);
    }

    private TransactionDto updateToSuccessTransaction(TransactionDto dto) {
        TransactionEntity entity = repository.findById(dto.getTransactionId())
                .orElseThrow(() -> new NoSuchElementException("Transaction not found"));

        entity.setStatus(TransactionStatus.SUCCESS);
        entity = repository.save(entity);

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return mapper.map(entity, TransactionDto.class);
    }

    private TransactionDto updateToFailedTransaction(TransactionDto dto) {
        TransactionEntity entity = repository.findById(dto.getTransactionId())
                .orElseThrow(() -> new NoSuchElementException("Transaction not found"));

        entity.setStatus(TransactionStatus.FAILED);
        entity = repository.save(entity);

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return mapper.map(entity, TransactionDto.class);
    }

    public TransactionDto getTransaction(Integer transactionId) throws NoSuchElementException {
        TransactionEntity entity = repository.findById(transactionId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found with ID: " + transactionId));

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return mapper.map(entity, TransactionDto.class);
    }

    public List<TransactionDto> getTransactions(Integer accountId) {
        List<TransactionEntity> entities = repository.findByAccountId(accountId);

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return entities.stream()
                .map(entity -> mapper.map(entity, TransactionDto.class))
                .toList();
    }

    private void updateAccount(TransactionDto transaction) {
        switch (transaction.getType()) {
            case DEPOSIT -> updateAccountForDepositTransaction(transaction);
            case WITHDRAW -> updateAccountForWithdrawTransaction(transaction);
            case TRANSFER -> updateAccountForTransferTransaction(transaction);
        }
    }

    private void updateAccountForDepositTransaction(TransactionDto transaction) {
        Integer accountId = transaction.getToAccountId();
        if (accountId == null) {
            throw new IllegalArgumentException("Deposit destination account ID cannot be null");
        }

        Long amount = transaction.getAmount();
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("account-service");

        AccountResponse response = circuitBreaker.run(
                () -> accountService.getAccount(accountId),
                throwable -> {
                    log.error("Circuit breaker triggered while getting account {}: {}", accountId, throwable.getMessage());
                    throw new IllegalStateException("Account service unavailable for account " + accountId, throwable);
                }
        );

        if (response.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Cannot deposit to non-active account: " + response.getStatus());
        }

        AccountRequest request = AccountRequest.builder()
                .balance(response.getBalance() + amount)
                .build();

        circuitBreaker.run(
                () -> accountService.updateAccount(accountId, request),
                throwable -> {
                    log.error("Circuit breaker triggered while updating account {}: {}", accountId, throwable.getMessage());
                    throw new IllegalStateException("Account service unavailable for update " + accountId, throwable);
                }
        );
    }

    private void updateAccountForWithdrawTransaction(TransactionDto transaction) {
        Integer accountId = transaction.getFromAccountId();
        if (accountId == null) {
            throw new IllegalArgumentException("Withdraw source account ID cannot be null");
        }

        Long amount = transaction.getAmount();
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("account-service");

        AccountResponse response = circuitBreaker.run(
                () -> accountService.getAccount(accountId),
                throwable -> {
                    log.error("Circuit breaker triggered while getting account {}: {}", accountId, throwable.getMessage());
                    throw new IllegalStateException("Account service unavailable for account " + accountId, throwable);
                }
        );

        if (response.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Cannot withdraw from non-active account: " + response.getStatus());
        }

        if (response.getBalance() < amount) {
            throw new IllegalStateException("Insufficient balance in account " + accountId + ". Current: " + response.getBalance() + ", Requested: " + amount);
        }

        AccountRequest request = AccountRequest.builder()
                .balance(response.getBalance() - amount)
                .build();

        circuitBreaker.run(
                () -> accountService.updateAccount(accountId, request),
                throwable -> {
                    log.error("Circuit breaker triggered while updating account {}: {}", accountId, throwable.getMessage());
                    throw new IllegalStateException("Account service unavailable for update " + accountId, throwable);
                }
        );
    }

    /**
     * Saga 분산 트랜잭션 패턴 적용 (단순 보상 트랜잭션):
     * Step 1: 출금 계좌에서 차감 (잔액 검증 포함)
     * Step 2: 입금 계좌로 증액
     * Step 3: 만약 Step 2(입금) 실패 시 보상 트랜잭션(compensateWithdraw)을 실행하여 출금 계좌로 원복
     */
    private void updateAccountForTransferTransaction(TransactionDto transaction) {
        Integer fromAccountId = transaction.getFromAccountId();
        Integer toAccountId = transaction.getToAccountId();

        if (fromAccountId == null || toAccountId == null) {
            throw new IllegalArgumentException("Both fromAccountId and toAccountId must be provided for transfer");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Step 1: 출금 처리
        updateAccountForWithdrawTransaction(transaction);

        // Step 2: 입금 처리 (실패 시 보상 트랜잭션 실행)
        try {
            updateAccountForDepositTransaction(transaction);
        } catch (Exception e) {
            log.error("Saga transfer failed at deposit step to account {}. Executing compensating transaction (re-deposit to account {})...",
                    toAccountId, fromAccountId, e);
            compensateWithdraw(fromAccountId, transaction.getAmount());
            throw new IllegalStateException("Transfer failed during deposit. Compensated withdrawal for account " + fromAccountId, e);
        }
    }

    private void compensateWithdraw(Integer accountId, Long amount) {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerFactory.create("account-service");
            AccountResponse response = circuitBreaker.run(
                    () -> accountService.getAccount(accountId),
                    throwable -> {
                        log.error("Compensation failed to get account {}: {}", accountId, throwable.getMessage());
                        return null;
                    }
            );

            if (response != null) {
                AccountRequest request = AccountRequest.builder()
                        .balance(response.getBalance() + amount)
                        .build();
                circuitBreaker.run(
                        () -> accountService.updateAccount(accountId, request),
                        throwable -> {
                            log.error("Compensation failed to update account {}: {}", accountId, throwable.getMessage());
                            return null;
                        }
                );
                log.info("Compensation completed: refunded {} to account {}", amount, accountId);
            }
        } catch (Exception ex) {
            log.error("Critical error during Saga compensation for account {}: {}", accountId, ex.getMessage(), ex);
        }
    }

    private void sendNotification(TransactionDto transaction) {
        String ownerName = resolveOwnerName(transaction);

        TransactionEvent.Builder builder = TransactionEvent.newBuilder();
        builder.setTransactionId(transaction.getTransactionId());
        builder.setOwnerName(ownerName);
        builder.setTransactionType(transaction.getType().toString());
        builder.setAmount(transaction.getAmount());
        builder.setCreatedAt(transaction.getCreatedAt() != null ? transaction.getCreatedAt().toString() : Instant.now().toString());
        builder.setFromAccountId(transaction.getFromAccountId());
        builder.setToAccountId(transaction.getToAccountId());
        builder.setStatus(transaction.getStatus() != null ? transaction.getStatus().toString() : "SUCCESS");
        TransactionEvent event = builder.build();

        ProducerRecord<String, TransactionEvent> record = new ProducerRecord<>(TOPIC, event);

        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("notification-service");
        circuitBreaker.run(
                () -> kafkaProducer.send(record),
                throwable -> {
                    log.warn("Failed to publish transaction event to Kafka: {}", throwable.getMessage());
                    return null;
                }
        );
    }

    private String resolveOwnerName(TransactionDto transaction) {
        try {
            Integer targetAccountId = transaction.getFromAccountId() != null ? transaction.getFromAccountId() : transaction.getToAccountId();
            if (targetAccountId != null) {
                AccountResponse account = accountService.getAccount(targetAccountId);
                if (account != null && account.getOwnerName() != null) {
                    return account.getOwnerName();
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve ownerName for transaction {}: {}", transaction.getTransactionId(), e.getMessage());
        }
        return "Unknown";
    }
}