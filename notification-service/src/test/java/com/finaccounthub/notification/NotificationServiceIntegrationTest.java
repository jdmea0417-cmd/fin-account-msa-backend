package com.finaccounthub.notification;

import com.finaccount.transactionservice.TransactionEvent;
import com.finaccounthub.notification.controller.NotificationController;
import com.finaccounthub.notification.entity.NotificationEntity;
import com.finaccounthub.notification.repository.NotificationRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"fin.transaction.events.test"})
@DirtiesContext
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationController notificationController;

    @Autowired
    private org.springframework.kafka.test.EmbeddedKafkaBroker embeddedKafkaBroker;

    @Value("${kafka.topic.transaction-events}")
    private String topic;

    private static final AtomicInteger TX_ID_SEQ = new AtomicInteger(900_000);

    private static int nextTransactionId() {
        return TX_ID_SEQ.incrementAndGet();
    }

    private KafkaTemplate<String, Object> testProducerTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put("schema.registry.url", "mock://test-scope");
        ProducerFactory<String, Object> pf = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(pf);
    }

    private TransactionEvent buildEvent(int transactionId, String ownerName, String type,
                                         long amount, Integer from, Integer to, String status) {
        return TransactionEvent.newBuilder()
                .setTransactionId(transactionId)
                .setOwnerName(ownerName)
                .setTransactionType(type)
                .setAmount(amount)
                .setCreatedAt(Instant.now().toString())
                .setFromAccountId(from)
                .setToAccountId(to)
                .setStatus(status)
                .build();
    }

    @Test
    void producerToConsumerToQueryApi_endToEnd() {
        int transactionId = nextTransactionId();
        int accountId = 1001;

        TransactionEvent event = buildEvent(transactionId, "Alice", "DEPOSIT", 50_000L,
                null, accountId, "SUCCESS");

        testProducerTemplate().send(topic, String.valueOf(accountId), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(notificationRepository.existsByTransactionId(transactionId)).isTrue()
        );

        List<NotificationEntity> all = notificationController.getAllNotifications();
        NotificationEntity saved = all.stream()
                .filter(n -> n.getTransactionId().equals(transactionId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("저장된 알림을 찾을 수 없음: " + transactionId));

        assertThat(saved.getTransactionType()).isEqualTo("DEPOSIT");
        assertThat(saved.getAmount()).isEqualTo(50_000L);
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getToAccountId()).isEqualTo(accountId);
        assertThat(saved.getMessage()).contains(String.valueOf(accountId)).contains("입금");
        assertThat(saved.getIsSuspicious()).isFalse();

        List<NotificationEntity> byUser = notificationController.getNotificationsByUser("Alice");
        assertThat(byUser).anyMatch(n -> n.getTransactionId().equals(transactionId));
    }

    @Test
    void duplicateTransactionId_isNotSavedTwice_idempotency() {
        int transactionId = nextTransactionId();
        int accountId = 2002;

        TransactionEvent event = buildEvent(transactionId, "Bob", "WITHDRAW", 10_000L,
                accountId, null, "SUCCESS");

        KafkaTemplate<String, Object> producer = testProducerTemplate();

        producer.send(topic, String.valueOf(accountId), event);
        producer.send(topic, String.valueOf(accountId), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(notificationRepository.existsByTransactionId(transactionId)).isTrue()
        );

        await().pollDelay(2, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            long count = notificationRepository.findAllByOrderByReceivedAtDesc().stream()
                    .filter(n -> n.getTransactionId().equals(transactionId))
                    .count();
            assertThat(count).isEqualTo(1);
        });
    }

    @Test
    void transferEvent_showsFromAndToAccountInMessage() {
        int transactionId = nextTransactionId();
        int from = 3001;
        int to = 3002;

        TransactionEvent event = buildEvent(transactionId, "Charlie", "TRANSFER", 30_000L, from, to, "SUCCESS");
        testProducerTemplate().send(topic, String.valueOf(from), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(notificationRepository.existsByTransactionId(transactionId)).isTrue()
        );

        NotificationEntity saved = notificationRepository.findAllByOrderByReceivedAtDesc().stream()
                .filter(n -> n.getTransactionId().equals(transactionId))
                .findFirst()
                .orElseThrow();

        assertThat(saved.getFromAccountId()).isEqualTo(from);
        assertThat(saved.getToAccountId()).isEqualTo(to);
        assertThat(saved.getMessage()).contains(String.valueOf(from)).contains(String.valueOf(to));
    }

    @Test
    void highAmountTransaction_triggersFdsSuspiciousAlert() {
        int transactionId = nextTransactionId();
        int accountId = 4001;
        long highAmount = 15_000_000L; // 1,500만 원 (임계치 1,000만 초과)

        TransactionEvent event = buildEvent(transactionId, "Dave", "WITHDRAW", highAmount,
                accountId, null, "SUCCESS");

        testProducerTemplate().send(topic, String.valueOf(accountId), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(notificationRepository.existsByTransactionId(transactionId)).isTrue()
        );

        NotificationEntity saved = notificationRepository.findAllByOrderByReceivedAtDesc().stream()
                .filter(n -> n.getTransactionId().equals(transactionId))
                .findFirst()
                .orElseThrow();

        assertThat(saved.getIsSuspicious()).isTrue();
        assertThat(saved.getMessage()).contains("[FDS 이상거래 의심 경고");
        assertThat(saved.getMessage()).contains("고액거래");

        List<NotificationEntity> suspiciousList = notificationController.getSuspiciousNotifications();
        assertThat(suspiciousList).anyMatch(n -> n.getTransactionId().equals(transactionId));
    }
}
