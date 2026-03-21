package com.ticketbooking.ticket.repository;

import com.ticketbooking.ticket.model.Ticket;
import com.ticketbooking.ticket.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {

    List<Ticket> findByUserId(String userId);

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByUserIdAndStatus(String userId, TicketStatus status);
}
