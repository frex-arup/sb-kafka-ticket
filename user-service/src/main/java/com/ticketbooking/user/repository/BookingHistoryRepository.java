package com.ticketbooking.user.repository;

import com.ticketbooking.user.model.BookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingHistoryRepository extends JpaRepository<BookingHistory, String> {
    List<BookingHistory> findByUserId(String userId);
    Optional<BookingHistory> findByTicketId(String ticketId);
}
