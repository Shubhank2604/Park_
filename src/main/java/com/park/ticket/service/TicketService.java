package com.park.ticket.service;

import com.park.common.dto.EntryRequest;
import com.park.common.dto.ExitRequest;
import com.park.common.events.EntryEvent;
import com.park.common.events.ExitEvent;
import com.park.common.events.SlotUpdated;
import com.park.lot.entity.ParkingLot;
import com.park.lot.entity.Slot;
import com.park.lot.repository.ParkingLotRepository;
import com.park.lot.repository.SlotRepository;
import com.park.lot.service.SlotCache;
import com.park.outbox.service.OutboxService;
import com.park.ticket.entity.Ticket;
import com.park.ticket.repository.TicketRepository;
import com.park.vehicle.entity.Vehicle;
import com.park.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TicketService {
    
    private final TicketRepository ticketRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final SlotRepository slotRepository;
    private final SlotCache slotCache;
    private final OutboxService outboxService;
    
    @Transactional
    public Map<String, Object> processEntry(EntryRequest request) {
        // Get or create vehicle
        Vehicle vehicle = vehicleRepository.findByPlateNo(request.getPlateNo())
                .orElseGet(() -> {
                    Vehicle newVehicle = new Vehicle();
                    newVehicle.setPlateNo(request.getPlateNo());
                    newVehicle.setType(Vehicle.VehicleType.valueOf(request.getVehicleType().toUpperCase()));
                    return vehicleRepository.save(newVehicle);
                });
        
        // Check if vehicle already has an open ticket
        if (ticketRepository.findOpenTicketByPlate(request.getPlateNo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle already has an open ticket");
        }
        
        // Get parking lot
        ParkingLot lot = parkingLotRepository.findById(request.getLotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking lot not found"));
        
        String vehicleType = request.getVehicleType().toUpperCase();
        Slot slot = slotRepository
                .findFirstByLevelParkingLotIdAndTypeAndStatusOrderByLevelLevelNoAscIdAsc(
                        request.getLotId(), Slot.SlotType.valueOf(vehicleType), Slot.SlotStatus.FREE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No available slots"));
        
        // Create ticket
        Ticket ticket = new Ticket();
        ticket.setParkingLot(lot);
        ticket.setSlot(slot);
        ticket.setVehicle(vehicle);
        ticket.setEntryTime(Instant.now());
        ticket.setStatus(Ticket.TicketStatus.OPEN);
        
        // Update slot status
        slot.setStatus(Slot.SlotStatus.OCCUPIED);
        
        // Save everything
        ticket = ticketRepository.save(ticket);
        slotRepository.save(slot);
        
        Ticket savedTicket = ticket;
        Map<String, String> ticketSummary = Map.of(
            "slotId", slot.getId().toString(),
            "lotId", lot.getId().toString(),
            "entryTime", ticket.getEntryTime().toString(),
            "plate", request.getPlateNo()
        );
        outboxService.record("parking.entry", savedTicket.getId().toString(),
                new EntryEvent(savedTicket.getId(), lot.getId(), slot.getId(),
                        request.getPlateNo(), vehicleType, savedTicket.getEntryTime()));
        outboxService.record("slot.updated", slot.getId().toString(),
                new SlotUpdated(slot.getId(), lot.getId(), vehicleType, "OCCUPIED"));
        afterCommit(() -> {
            slotCache.markOccupied(request.getLotId(), slot.getId(), vehicleType);
            slotCache.cacheOpenTicket(request.getPlateNo(), savedTicket.getId());
            slotCache.cacheTicketSummary(savedTicket.getId(), ticketSummary);
        });
        
        return Map.of(
            "ticketId", ticket.getId(),
            "slotCode", slot.getCode(),
            "level", slot.getLevel().getLevelNo(),
            "entryTime", ticket.getEntryTime()
        );
    }
    
    @Transactional
    public Map<String, Object> processExit(ExitRequest request) {
        // Find open ticket
        Ticket ticket = ticketRepository.findOpenTicketById(Long.parseLong(request.getIdentifier()))
                .orElse(ticketRepository.findOpenTicketByPlate(request.getIdentifier())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No open ticket found")));
        
        // Update ticket
        ticket.setExitTime(Instant.now());
        ticket.setStatus(Ticket.TicketStatus.CLOSED);
        ticketRepository.save(ticket);
        
        // Free the slot
        Slot slot = ticket.getSlot();
        slot.setStatus(Slot.SlotStatus.FREE);
        slotRepository.save(slot);
        
        ExitEvent exitEvent = new ExitEvent(
            ticket.getId(),
            ticket.getParkingLot().getId(),
            slot.getId(),
            ticket.getVehicle().getPlateNo(),
            slot.getType().name(),
            ticket.getEntryTime(),
            ticket.getExitTime()
        );
        outboxService.record("parking.exit", ticket.getId().toString(), exitEvent);
        outboxService.record("slot.updated", slot.getId().toString(),
                new SlotUpdated(slot.getId(), ticket.getParkingLot().getId(), slot.getType().name(), "FREE"));
        afterCommit(() -> {
            slotCache.markFree(ticket.getParkingLot().getId(), slot.getId(), slot.getType().name());
            slotCache.removeOpenTicket(ticket.getVehicle().getPlateNo());
        });
        
        return Map.of(
            "ticketId", ticket.getId(),
            "exitTime", ticket.getExitTime(),
            "billing", "processing"
        );
    }

    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
