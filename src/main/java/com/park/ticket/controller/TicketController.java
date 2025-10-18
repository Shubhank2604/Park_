package com.park.ticket.controller;

import com.park.common.dto.EntryRequest;
import com.park.common.dto.ExitRequest;
import com.park.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TicketController {
    
    private final TicketService ticketService;
    
    @PostMapping("/api/entry")
    public ResponseEntity<Map<String, Object>> processEntry(@Valid @RequestBody EntryRequest request) {
        Map<String, Object> response = ticketService.processEntry(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/api/exit")
    public ResponseEntity<Map<String, Object>> processExit(@Valid @RequestBody ExitRequest request) {
        Map<String, Object> response = ticketService.processExit(request);
        return ResponseEntity.ok(response);
    }
}
