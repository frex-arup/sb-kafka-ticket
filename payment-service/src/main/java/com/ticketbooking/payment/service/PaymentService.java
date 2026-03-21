package com.ticketbooking.payment.service;

import com.ticketbooking.common.event.PaymentCompletedEvent;
import com.ticketbooking.common.event.PaymentFailedEvent;
import com.ticketbooking.common.event.TicketReservedEvent;
import com.ticketbooking.payment.model.Payment;
import com.ticketbooking.payment.model.PaymentStatus;
import com.ticketbooking.payment.producer.PaymentEventProducer;
import com.ticketbooking.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

/**
 * Payment Service - Processes payments for ticket reservations.
 *
 * This simulates a payment gateway with configurable success rate.
 * In production, this would integrate with real payment providers
 * like Stripe, PayPal, etc.
 *
 * Learning Note: The service randomly fails some payments to demonstrate
 * the compensating transaction pattern - how the system handles failures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;
    private final Random random = new Random();

    @Value("${payment.success-rate}")
    private double successRate;

    /**
     * Process payment for a ticket reservation.
     * Simulates payment with configurable success rate.
     */
    @Transactional
    public void processPayment(TicketReservedEvent event) {
        log.info("Processing payment for ticket: {}, amount: {}, correlationId: {}",
                event.getTicketId(), event.getTotalAmount(), event.getCorrelationId());

        // Create payment record
        Payment payment = Payment.builder()
                .ticketId(event.getTicketId())
                .userId(event.getUserId())
                .amount(event.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .paymentMethod("CREDIT_CARD")
                .correlationId(event.getCorrelationId())
                .build();

        payment = paymentRepository.save(payment);

        // NOTE: Thread.sleep removed for better performance
        // In production, this would be async payment gateway call
        // Keeping minimal delay to simulate network latency
        try {
            Thread.sleep(100); // Reduced to 100ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate payment gateway response (80% success by default)
        boolean paymentSuccessful = random.nextDouble() < successRate;

        if (paymentSuccessful) {
            handleSuccessfulPayment(payment);
        } else {
            handleFailedPayment(payment);
        }
    }

    private void handleSuccessfulPayment(Payment payment) {
        log.info("Payment successful: paymentId={}", payment.getId());

        // Update payment record
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString());
        paymentRepository.save(payment);

        // Publish payment completed event
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                payment.getId(),
                payment.getTicketId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getUserId(),
                payment.getTransactionId(),
                payment.getCorrelationId()
        );

        eventProducer.publishPaymentCompleted(event);
    }

    private void handleFailedPayment(Payment payment) {
        String failureReason = "Payment declined by gateway";
        log.warn("Payment failed: paymentId={}, reason={}", payment.getId(), failureReason);

        // Update payment record
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        paymentRepository.save(payment);

        // Publish payment failed event (triggers compensating transaction)
        PaymentFailedEvent event = new PaymentFailedEvent(
                payment.getId(),
                payment.getTicketId(),
                payment.getAmount(),
                payment.getUserId(),
                failureReason,
                payment.getCorrelationId()
        );

        eventProducer.publishPaymentFailed(event);
    }
}
