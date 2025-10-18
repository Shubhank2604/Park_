package com.park.common.service;

import com.park.common.events.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishEntry(EntryEvent event) {
        kafkaTemplate.send("parking.entry", event.ticketId().toString(), event);
    }
    
    public void publishExit(ExitEvent event) {
        kafkaTemplate.send("parking.exit", event.ticketId().toString(), event);
    }
    
    public void publishSlot(SlotUpdated event) {
        kafkaTemplate.send("slot.updated", event.slotId().toString(), event);
    }
    
    public void publishInvoiceCreated(InvoiceCreated event) {
        kafkaTemplate.send("billing.invoice.created", event.invoiceId().toString(), event);
    }
    
    public void publishPaymentCompleted(PaymentCompleted event) {
        kafkaTemplate.send("payment.completed", event.invoiceId().toString(), event);
    }
}
