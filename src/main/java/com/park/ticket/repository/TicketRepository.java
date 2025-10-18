package com.park.ticket.repository;

import com.park.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @Query("SELECT t FROM Ticket t WHERE t.vehicle.plateNo = :plateNo AND t.status = 'OPEN'")
    Optional<Ticket> findOpenTicketByPlate(@Param("plateNo") String plateNo);
    
    @Query("SELECT t FROM Ticket t WHERE t.id = :ticketId AND t.status = 'OPEN'")
    Optional<Ticket> findOpenTicketById(@Param("ticketId") Long ticketId);
}
