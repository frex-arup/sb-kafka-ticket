package com.ticketbooking.common.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared Kafka Producer configuration for all services.
 *
 * Key configurations explained:
 * - ACKS_CONFIG=all: Wait for all replicas to acknowledge (durability)
 * - RETRIES_CONFIG=3: Retry failed sends up to 3 times
 * - ENABLE_IDEMPOTENCE_CONFIG=true: Prevents duplicate messages during retries
 * - MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION=5: Allows batching for performance
 *
 * Learning Note: These settings provide a good balance between reliability
 * and performance. In production, you might tune based on specific needs.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Kafka broker address
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Key serializer - converts key to bytes
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Value serializer - converts Java objects to JSON
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // Retry failed sends
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Exactly-once semantics

        // Performance settings
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // Compress messages

        // Batching for better throughput
        // Reduced linger for low-latency event-driven system
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1); // 1ms (was 10ms)
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB batch size

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
