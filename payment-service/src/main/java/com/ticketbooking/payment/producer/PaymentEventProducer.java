package com.ticketbooking.payment.producer;

import com.ticketbooking.common.event.PaymentCompletedEvent;
import com.ticketbooking.common.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-events}")
    private String paymentEventsTopic;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Publishing PaymentCompletedEvent: paymentId={}, ticketId={}, correlationId={}",
                event.getPaymentId(), event.getTicketId(), event.getCorrelationId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(paymentEventsTopic, event.getTicketId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("PaymentCompletedEvent published successfully");
            } else {
                log.error("Failed to publish PaymentCompletedEvent", ex);
            }
        });
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.info("Publishing PaymentFailedEvent: paymentId={}, ticketId={}, correlationId={}",
                event.getPaymentId(), event.getTicketId(), event.getCorrelationId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(paymentEventsTopic, event.getTicketId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("PaymentFailedEvent published successfully");
            } else {
                log.error("Failed to publish PaymentFailedEvent", ex);
            }
        });
    }
}
