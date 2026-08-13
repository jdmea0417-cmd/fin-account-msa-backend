package com.finaccounthub.notification.listener;

import com.finaccounthub.avro.TransactionEvent;
import com.finaccounthub.notification.entity.NotificationEntity;
import com.finaccounthub.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * fin.transaction.events 토픽을 구독하여 거래 이벤트를 알림 로그로 변환/저장한다.
 *
 * Avro + Schema Registry 덕분에 컨슈머는 스키마 정의(TransactionEvent) 그대로
 * 타입 안전하게 역직렬화된 객체를 수신한다. (KafkaAvroDeserializer가 자동 처리)
 */
@Component
public class TransactionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListener.class);

    private final NotificationRepository notificationRepository;

    public TransactionEventListener(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @KafkaListener(topics = "${kafka.topic.transaction-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onTransactionEvent(TransactionEvent event) {
        log.info("Received TransactionEvent: {}", event);

        String message = buildMessage(event);

        NotificationEntity entity = new NotificationEntity(
                event.getTransactionId().toString(),
                event.getAccountId().toString(),
                event.getUserId().toString(),
                event.getTransactionType().toString(),
                event.getAmount(),
                message,
                LocalDateTime.now()
        );

        notificationRepository.save(entity);
    }

    private String buildMessage(TransactionEvent event) {
        return switch (event.getTransactionType().toString()) {
            case "DEPOSIT" -> String.format("계좌 %s에 %,d원이 입금되었습니다.", event.getAccountId(), event.getAmount());
            case "WITHDRAWAL" -> String.format("계좌 %s에서 %,d원이 출금되었습니다.", event.getAccountId(), event.getAmount());
            case "TRANSFER" -> String.format("계좌 %s에서 %,d원이 이체되었습니다.", event.getAccountId(), event.getAmount());
            default -> String.format("계좌 %s에 거래(%s) %,d원이 발생했습니다.", event.getAccountId(), event.getTransactionType(), event.getAmount());
        };
    }
}
