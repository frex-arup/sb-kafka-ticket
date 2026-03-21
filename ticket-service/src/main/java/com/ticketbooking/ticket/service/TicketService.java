package com.ticketbooking.ticket.service;

import com.ticketbooking.common.event.*;
import com.ticketbooking.common.exception.ResourceNotFoundException;
import com.ticketbooking.ticket.dto.TicketRequestDto;
import com.ticketbooking.ticket.dto.TicketResponseDto;
import com.ticketbooking.ticket.model.Ticket;
import com.ticketbooking.ticket.model.TicketStatus;
import com.ticketbooking.ticket.producer.TicketEventProducer;
import com.ticketbooking.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Ticket Service - Core business logic for ticket management.
 *
 * This service orchestrates the booking SAGA:
 * 1. Reserve ticket → Publish TicketReservedEvent
 * 2. Wait for payment event
 * 3a. If PaymentCompleted → Confirm booking → Publish TicketBookedEvent
 * 3b. If PaymentFailed → Release ticket → Publish TicketReleasedEvent
 *
 * Learning Note: This shows how business logic and event publishing
 * work together to implement distributed transactions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketEventProducer eventProducer;

    @Value("${ticket-booking.reservation-timeout-minutes}")
    private int reservationTimeoutMinutes;

    /**
     * Reserve a ticket - Step 1 of SAGA.
     * Save to DB and publish event to Kafka.
     */
    @Transactional
    public TicketResponseDto reserveTicket(TicketRequestDto request) {
        log.info("Reserving ticket for user: {}, movie: {}", request.getUserId(), request.getMovieName());

        // Create correlation ID for tracing this booking across services
        String correlationId = UUID.randomUUID().toString();

        // Calculate reservation expiry
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(reservationTimeoutMinutes);

        // Create and save ticket
        Ticket ticket = Ticket.builder()
                .movieName(request.getMovieName())
                .showTime(request.getShowTime())
                .seatNumbers(request.getSeatNumbers())
                .userId(request.getUserId())
                .totalAmount(request.getTotalAmount())
                .status(TicketStatus.RESERVED)
                .reservedUntil(reservedUntil)
                .correlationId(correlationId)
                .build();

        ticket = ticketRepository.save(ticket);
        log.info("Ticket reserved in DB: id={}", ticket.getId());

        // Publish event to Kafka - this triggers payment processing
        TicketReservedEvent event = TicketReservedEvent.builder()
                .eventType("ticket.reserved")
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .ticketId(ticket.getId())
                .movieName(ticket.getMovieName())
                .showTime(ticket.getShowTime())
                .seatNumbers(ticket.getSeatNumbers())
                .userId(ticket.getUserId())
                .totalAmount(ticket.getTotalAmount())
                .reservedUntil(ticket.getReservedUntil())
                .paymentProvider(request.getPaymentProvider() != null ? request.getPaymentProvider() : "SIMULATED")
                .build();

        eventProducer.publishTicketReserved(event);

        return mapToDto(ticket);
    }

    /**
     * Confirm booking - Step 3a of SAGA (after successful payment).
     * Update status and publish confirmation event.
     */
    @Transactional
    public void confirmBooking(PaymentCompletedEvent event) {
        log.info("Confirming booking for ticket: {}", event.getTicketId());

        Ticket ticket = ticketRepository.findById(event.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", event.getTicketId()));

        // Validate ticket is in expected state (idempotency check)
        if (ticket.getStatus() == TicketStatus.BOOKED) {
            log.info("Ticket {} already booked, skipping duplicate event", ticket.getId());
            return;
        }

        if (ticket.getStatus() != TicketStatus.RESERVED) {
            log.warn("Cannot confirm ticket {} in status {}", ticket.getId(), ticket.getStatus());
            return;
        }

        // Update ticket status
        ticket.setStatus(TicketStatus.BOOKED);
        ticket.setConfirmationCode(generateConfirmationCode());
        ticketRepository.save(ticket);

        log.info("Ticket booking confirmed: id={}, confirmationCode={}",
                ticket.getId(), ticket.getConfirmationCode());

        // Publish booking confirmed event
        TicketBookedEvent bookedEvent = new TicketBookedEvent(
                UUID.randomUUID().toString(), // booking ID
                ticket.getId(),
                ticket.getUserId(),
                ticket.getConfirmationCode(),
                "CONFIRMED",
                event.getCorrelationId()
        );

        eventProducer.publishTicketBooked(bookedEvent);
    }

    /**
     * Release ticket - Step 3b of SAGA (compensating transaction after payment failure).
     * Update status and publish release event.
     */
    @Transactional
    public void releaseTicket(PaymentFailedEvent event) {
        log.info("Releasing ticket due to payment failure: {}", event.getTicketId());

        Ticket ticket = ticketRepository.findById(event.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", event.getTicketId()));

        // Update ticket status
        ticket.setStatus(TicketStatus.RELEASED);
        ticketRepository.save(ticket);

        log.info("Ticket released: id={}, reason={}", ticket.getId(), event.getFailureReason());

        // Publish ticket released event
        TicketReleasedEvent releasedEvent = new TicketReleasedEvent(
                ticket.getId(),
                ticket.getUserId(),
                "PAYMENT_FAILED: " + event.getFailureReason(),
                event.getCorrelationId()
        );

        eventProducer.publishTicketReleased(releasedEvent);
    }

    /**
     * Get ticket by ID.
     */
    public TicketResponseDto getTicketById(String id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
        return mapToDto(ticket);
    }

    /**
     * Get all tickets for a user.
     */
    public List<TicketResponseDto> getTicketsByUserId(String userId) {
        return ticketRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDto)
                .toList(); // Java 16+ simplified syntax
    }

    /**
     * Get all tickets with pagination to prevent memory issues.
     * @deprecated Use paginated version for better performance
     */
    @Deprecated
    public List<TicketResponseDto> getAllTickets() {
        // WARNING: Unbounded query - use with caution in production
        return ticketRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .toList(); // Java 16+ simplified syntax
    }

    private String generateConfirmationCode() {
        return "TKT" + System.currentTimeMillis();
    }

    private TicketResponseDto mapToDto(Ticket ticket) {
        return TicketResponseDto.builder()
                .id(ticket.getId())
                .movieName(ticket.getMovieName())
                .showTime(ticket.getShowTime())
                .seatNumbers(ticket.getSeatNumbers())
                .userId(ticket.getUserId())
                .totalAmount(ticket.getTotalAmount())
                .status(ticket.getStatus())
                .reservedUntil(ticket.getReservedUntil())
                .confirmationCode(ticket.getConfirmationCode())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
