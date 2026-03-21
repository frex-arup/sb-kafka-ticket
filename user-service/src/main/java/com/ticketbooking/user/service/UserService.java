package com.ticketbooking.user.service;

import com.ticketbooking.common.event.*;
import com.ticketbooking.common.exception.ResourceNotFoundException;
import com.ticketbooking.user.dto.UserRequestDto;
import com.ticketbooking.user.model.BookingHistory;
import com.ticketbooking.user.model.User;
import com.ticketbooking.user.producer.UserEventProducer;
import com.ticketbooking.user.repository.BookingHistoryRepository;
import com.ticketbooking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final UserEventProducer eventProducer;

    @Transactional
    public User createUser(UserRequestDto dto) {
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .build();

        user = userRepository.save(user);
        log.info("User created: {}", user.getId());

        UserRegisteredEvent event = new UserRegisteredEvent(
                user.getId(), user.getName(), user.getEmail(), UUID.randomUUID().toString()
        );
        eventProducer.publishUserRegistered(event);

        return user;
    }

    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<BookingHistory> getUserBookings(String userId) {
        return bookingHistoryRepository.findByUserId(userId);
    }

    @Transactional
    public void handleTicketBooked(TicketBookedEvent event) {
        BookingHistory history = bookingHistoryRepository.findByTicketId(event.getTicketId())
                .orElseGet(() -> BookingHistory.builder()
                        .userId(event.getUserId())
                        .ticketId(event.getTicketId())
                        .build());

        history.setBookingId(event.getBookingId());
        history.setStatus("BOOKED");
        history.setConfirmationCode(event.getConfirmationCode());

        bookingHistoryRepository.save(history);
        log.info("Booking history updated: {}", history.getId());
    }

    @Transactional
    public void handleTicketReleased(TicketReleasedEvent event) {
        bookingHistoryRepository.findByTicketId(event.getTicketId())
                .ifPresent(history -> {
                    history.setStatus("RELEASED");
                    bookingHistoryRepository.save(history);
                    log.info("Booking marked as released: {}", history.getId());
                });
    }
}
