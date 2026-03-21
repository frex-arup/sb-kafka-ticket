package com.ticketbooking.payment.repository;

import com.ticketbooking.payment.model.Payment;
import com.ticketbooking.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByTicketId(String ticketId);

    List<Payment> findByUserId(String userId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByPaymentGatewayOrderId(String paymentGatewayOrderId);
}
