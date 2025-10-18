package com.park.billing.service;

import com.park.billing.entity.Invoice;
import com.park.billing.entity.TariffRule;
import com.park.billing.repository.InvoiceRepository;
import com.park.billing.repository.TariffRuleRepository;
import com.park.common.events.ExitEvent;
import com.park.common.events.InvoiceCreated;
import com.park.common.events.PaymentCompleted;
import com.park.common.service.EventPublisher;
import com.park.ticket.entity.Ticket;
import com.park.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BillingService {
    
    private final InvoiceRepository invoiceRepository;
    private final TariffRuleRepository tariffRuleRepository;
    private final TicketRepository ticketRepository;
    private final EventPublisher eventPublisher;
    
    @KafkaListener(topics = "parking.exit", groupId = "billing")
    public void onExit(ExitEvent event) {
        generateInvoice(event);
    }
    
    @Transactional
    public Invoice generateInvoice(ExitEvent event) {
        Ticket ticket = ticketRepository.findById(event.ticketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        // Calculate amount
        long amountCents = calculateAmount(event.lotId(), event.entryTime(), event.exitTime(), event.type());
        
        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setTicket(ticket);
        invoice.setAmountCents(amountCents);
        invoice.setCurrency("USD");
        invoice.setGeneratedAt(Instant.now());
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        
        invoice = invoiceRepository.save(invoice);
        
        // Publish event
        eventPublisher.publishInvoiceCreated(new InvoiceCreated(
            invoice.getId(),
            ticket.getId(),
            amountCents,
            "USD"
        ));
        
        return invoice;
    }
    
    private long calculateAmount(Long lotId, Instant entryTime, Instant exitTime, String vehicleType) {
        TariffRule rule = tariffRuleRepository.findByLotId(lotId)
                .orElseGet(() -> {
                    // Default tariff rule
                    TariffRule defaultRule = new TariffRule();
                    defaultRule.setLotId(lotId);
                    return defaultRule;
                });
        
        long minutes = ChronoUnit.MINUTES.between(entryTime, exitTime);
        long amount = rule.getBaseCents();
        
        if (minutes > rule.getBaseMinutes()) {
            long extraMinutes = minutes - rule.getBaseMinutes();
            long steps = (extraMinutes + rule.getStepMinutes() - 1) / rule.getStepMinutes();
            amount += steps * rule.getStepCents();
        }
        
        // Add EV surcharge
        if ("EV".equals(vehicleType)) {
            amount += rule.getEvSurchargeCents();
        }
        
        return amount;
    }
    
    public List<Invoice> getInvoicesByTicket(Long ticketId) {
        return invoiceRepository.findByTicketId(ticketId);
    }
    
    @Transactional
    public Map<String, String> processPayment(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            return Map.of("status", "already_paid");
        }
        
        // Mock payment processing
        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoiceRepository.save(invoice);
        
        // Publish payment completed event
        eventPublisher.publishPaymentCompleted(new PaymentCompleted(
            invoice.getId(),
            invoice.getTicket().getId(),
            invoice.getAmountCents(),
            invoice.getCurrency()
        ));
        
        return Map.of("status", "paid", "amount", invoice.getAmount().toString());
    }
}
