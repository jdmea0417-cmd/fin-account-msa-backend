package com.finaccount.transactionservice.config;

import com.finaccount.transactionservice.TransactionEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class KafkaConfig {
    @Bean
    public KafkaProducer<String, TransactionEvent> kafkaProducer(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();

        return new KafkaProducer<>(producerProperties);
    }
}
