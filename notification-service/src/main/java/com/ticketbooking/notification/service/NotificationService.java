package com.ticketbooking.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Notification Service - Simulates sending emails/SMS.
 *
 * In production, this would integrate with email services (SendGrid, AWS SES)
 * and SMS services (Twilio, AWS SNS).
 *
 * For learning purposes, we just log the notifications.
 */
@Service
@Slf4j
public class NotificationService {

    public void sendReservationConfirmation(String userId, String ticketId, String movieName) {
        log.info("📧 [EMAIL] Sending reservation confirmation to user: {}", userId);
        log.info("   Ticket ID: {}, Movie: {}", ticketId, movieName);
        log.info("   Message: Your tickets have been reserved. Please complete payment within 10 minutes.");
        // In production: call email service API
    }

    public void sendPaymentConfirmation(String userId, String ticketId, String transactionId) {
        log.info("📧 [EMAIL] Sending payment confirmation to user: {}", userId);
        log.info("   Ticket ID: {}, Transaction ID: {}", ticketId, transactionId);
        log.info("   Message: Payment successful! Your booking is being confirmed.");
        // In production: call email service API
    }

    public void sendBookingConfirmation(String userId, String confirmationCode, String movieName) {
        log.info("📧 [EMAIL] Sending booking confirmation to user: {}", userId);
        log.info("   Confirmation Code: {}, Movie: {}", confirmationCode, movieName);
        log.info("   Message: Booking confirmed! Show this code at the venue.");
        log.info("📱 [SMS] Sending SMS notification to user: {}", userId);
        // In production: call SMS service API
    }

    public void sendPaymentFailureNotification(String userId, String ticketId, String reason) {
        log.info("📧 [EMAIL] Sending payment failure notification to user: {}", userId);
        log.info("   Ticket ID: {}, Reason: {}", ticketId, reason);
        log.info("   Message: Payment failed. Your reservation has been cancelled. Please try again.");
        // In production: call email service API
    }

    public void sendWelcomeEmail(String userId, String name, String email) {
        log.info("📧 [EMAIL] Sending welcome email to: {} ({})", name, email);
        log.info("   Message: Welcome to Ticket Booking System!");
        // In production: call email service API
    }
}
