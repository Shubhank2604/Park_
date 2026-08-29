package com.park.billing.entity;

import com.park.ticket.entity.Ticket;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;
    
    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;
    
    @Column(nullable = false)
    private String currency = "USD";
    
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.PENDING;
    
    public enum InvoiceStatus {
        PENDING, PAID
    }
    
    public BigDecimal getAmount() {
        return BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100));
    }
    
    public void setAmount(BigDecimal amount) {
        this.amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
}
