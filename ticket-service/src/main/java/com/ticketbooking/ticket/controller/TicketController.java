package com.ticketbooking.ticket.controller;

import com.ticketbooking.ticket.dto.TicketRequestDto;
import com.ticketbooking.ticket.dto.TicketResponseDto;
import com.ticketbooking.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Ticket operations.
 *
 * Provides endpoints for:
 * - Reserving tickets (starts the SAGA)
 * - Querying ticket status
 * - Getting user's tickets
 *
 * Learning Note: This is the entry point for the event-driven flow.
 * The REST API receives synchronous requests, but then the system
 * processes them asynchronously via events.
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ticket API", description = "Endpoints for managing ticket reservations")
@CrossOrigin(origins = "*")
public class TicketController {

    private final TicketService ticketService;

    /**
     * Reserve tickets - Initiates the booking SAGA.
     */
    @PostMapping("/reserve")
    @Operation(summary = "Reserve tickets", description = "Create a ticket reservation and start the booking process")
    public ResponseEntity<TicketResponseDto> reserveTicket(@Valid @RequestBody TicketRequestDto request) {
        log.info("Reserve ticket request received: movie={}, user={}", request.getMovieName(), request.getUserId());
        TicketResponseDto response = ticketService.reserveTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get ticket by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get ticket by ID", description = "Retrieve ticket details by ID")
    public ResponseEntity<TicketResponseDto> getTicketById(@PathVariable String id) {
        TicketResponseDto response = ticketService.getTicketById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all tickets for a user.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user tickets", description = "Get all tickets for a specific user")
    public ResponseEntity<List<TicketResponseDto>> getTicketsByUserId(@PathVariable String userId) {
        List<TicketResponseDto> tickets = ticketService.getTicketsByUserId(userId);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get all tickets (for admin/testing).
     */
    @GetMapping
    @Operation(summary = "Get all tickets", description = "Get all tickets in the system")
    public ResponseEntity<List<TicketResponseDto>> getAllTickets() {
        List<TicketResponseDto> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }
}
