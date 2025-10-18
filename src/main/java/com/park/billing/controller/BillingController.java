package com.park.billing.controller;

import com.park.billing.entity.Invoice;
import com.park.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BillingController {
    
    private final BillingService billingService;
    
    @GetMapping("/invoices/{ticketId}")
    public ResponseEntity<List<Invoice>> getInvoicesByTicket(@PathVariable Long ticketId) {
        List<Invoice> invoices = billingService.getInvoicesByTicket(ticketId);
        return ResponseEntity.ok(invoices);
    }
    
    @PostMapping("/pay/{invoiceId}")
    public ResponseEntity<Map<String, String>> processPayment(@PathVariable Long invoiceId) {
        Map<String, String> result = billingService.processPayment(invoiceId);
        return ResponseEntity.ok(result);
    }
}
