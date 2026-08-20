package com.finaccounthub.notification.listener;

import com.finaccount.transactionservice.TransactionEvent;
import com.finaccounthub.notification.entity.NotificationEntity;
import com.finaccounthub.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * fin.transaction.events 토픽을 구독하여 거래 이벤트를 알림 로그로 변환/저장한다.
 * FDS(이상거래 탐지 시스템) 룰을 적용하여 고액 거래 및 단시간 다건 거래를 감지한다.
 */
@Component
public class TransactionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListener.class);

    // FDS 임계치 설정
    private static final long HIGH_AMOUNT_THRESHOLD = 10_000_000L; // 1,000만 원 이상
    private static final int RAPID_TX_THRESHOLD = 3; // 1분 내 3건 이상

    private final NotificationRepository notificationRepository;

    public TransactionEventListener(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @KafkaListener(topics = "${kafka.topic.transaction-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onTransactionEvent(TransactionEvent event) {
        log.info("Received TransactionEvent: {}", event);

        Integer transactionId = event.getTransactionId();

        // 멱등성 보장
        if (notificationRepository.existsByTransactionId(transactionId)) {
            log.warn("Duplicate TransactionEvent detected, skip saving: transactionId={}", transactionId);
            return;
        }

        String status = event.getStatus() != null ? event.getStatus().toString() : "SUCCESS";
        String ownerName = event.getOwnerName() != null ? event.getOwnerName().toString() : "Unknown";
        long amount = event.getAmount();

        // FDS 이상거래 탐지 로직
        boolean isSuspicious = false;
        StringBuilder fdsReason = new StringBuilder();

        if (amount >= HIGH_AMOUNT_THRESHOLD) {
            isSuspicious = true;
            fdsReason.append(String.format("[고액거래: %,d원] ", amount));
        }

        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long recentTxCount = notificationRepository.countRecentTransactionsByOwner(ownerName, oneMinuteAgo);
        if (recentTxCount >= RAPID_TX_THRESHOLD) {
            isSuspicious = true;
            fdsReason.append(String.format("[단시간 다건거래: 최근 1분간 %d건] ", recentTxCount));
        }

        String baseMessage = buildMessage(event, status);
        String finalMessage;
        if (isSuspicious) {
            finalMessage = String.format("[FDS 이상거래 의심 경고 %s] %s", fdsReason.toString().trim(), baseMessage);
            log.warn("🚨 [FDS ALERT] 이상거래 감지: transactionId={}, owner={}, amount={}, reason={}",
                    transactionId, ownerName, amount, fdsReason);
        } else {
            finalMessage = baseMessage;
        }

        NotificationEntity entity = new NotificationEntity(
                transactionId,
                ownerName,
                event.getTransactionType().toString(),
                amount,
                finalMessage,
                status,
                isSuspicious,
                event.getFromAccountId(),
                event.getToAccountId(),
                LocalDateTime.now()
        );

        try {
            notificationRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate TransactionEvent (unique constraint), skip saving: transactionId={}", transactionId);
        }
    }

    private String buildMessage(TransactionEvent event, String status) {
        Integer from = event.getFromAccountId();
        Integer to = event.getToAccountId();

        if ("FAILED".equals(status)) {
            return String.format("거래(%s) %,d원이 실패했습니다.", event.getTransactionType(), event.getAmount());
        }
        if ("PENDING".equals(status)) {
            return String.format("거래(%s) %,d원이 처리 중입니다.", event.getTransactionType(), event.getAmount());
        }

        return switch (event.getTransactionType().toString()) {
            case "DEPOSIT" -> String.format("계좌 %d에 %,d원이 입금되었습니다.", to, event.getAmount());
            case "WITHDRAW" -> String.format("계좌 %d에서 %,d원이 출금되었습니다.", from, event.getAmount());
            case "TRANSFER" -> String.format("계좌 %d에서 계좌 %d로 %,d원이 이체되었습니다.", from, to, event.getAmount());
            default -> String.format("거래(%s) %,d원이 발생했습니다.", event.getTransactionType(), event.getAmount());
        };
    }
}
