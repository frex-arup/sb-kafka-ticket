package com.ticketbooking.ticket.dto;

import com.ticketbooking.ticket.model.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDto {
    private String id;
    private String movieName;
    private LocalDateTime showTime;
    private List<String> seatNumbers;
    private String userId;
    private BigDecimal totalAmount;
    private TicketStatus status;
    private LocalDateTime reservedUntil;
    private String confirmationCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
