package com.ticketbooking.ticket.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequestDto {

    @NotBlank(message = "Movie name is required")
    private String movieName;

    @NotNull(message = "Show time is required")
    @Future(message = "Show time must be in the future")
    private LocalDateTime showTime;

    @NotEmpty(message = "At least one seat must be selected")
    @Size(max = 10, message = "Maximum 10 seats per booking")
    private List<String> seatNumbers;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal totalAmount;

    private String paymentProvider; // RAZORPAY, STRIPE, or SIMULATED
}
