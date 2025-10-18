package com.park.billing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tariff_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "lot_id", nullable = false)
    private Long lotId;
    
    @Column(name = "base_minutes", nullable = false)
    private Integer baseMinutes = 60;
    
    @Column(name = "base_cents", nullable = false)
    private Long baseCents = 500L; // $5.00
    
    @Column(name = "step_minutes", nullable = false)
    private Integer stepMinutes = 30;
    
    @Column(name = "step_cents", nullable = false)
    private Long stepCents = 200L; // $2.00
    
    @Column(name = "ev_surcharge_cents", nullable = false)
    private Long evSurchargeCents = 100L; // $1.00
}
