package com.ticketbooking.common.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared Kafka Consumer configuration for all services.
 *
 * Key configurations explained:
 * - AUTO_OFFSET_RESET_CONFIG=earliest: Start from beginning for new consumers
 * - ENABLE_AUTO_COMMIT_CONFIG=false: Manual commit for better control
 * - ErrorHandlingDeserializer: Wraps deserializer to handle bad messages
 * - Concurrency=3: Each listener runs 3 consumer threads
 *
 * Learning Note: Consumer groups allow horizontal scaling. Multiple instances
 * of a service share the load by consuming from different partitions.
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Kafka broker address
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Key deserializer - converts bytes to String
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Value deserializer with error handling
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        // JsonDeserializer configuration (prefixed for ErrorHandlingDeserializer)
        // TRUSTED_PACKAGES="*" allows deserializing any class (use cautiously in production)
        // USE_TYPE_INFO_HEADERS=true reads __TypeId__ header set by producer
        // No TYPE_MAPPINGS needed - producer sends fully qualified class names in headers
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        // Consumer behavior
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Start from beginning
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit

        // Performance settings
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // Max records per poll
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024); // Min bytes to fetch

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // Run 3 consumer threads per listener for parallelism
        factory.setConcurrency(3);

        // Manual acknowledgment mode - gives more control over when messages are marked as processed
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}
