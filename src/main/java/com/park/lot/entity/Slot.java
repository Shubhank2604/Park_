package com.park.lot.entity;

import com.park.ticket.entity.Ticket;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Slot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id", nullable = false)
    private Level level;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status = SlotStatus.FREE;
    
    @Column(nullable = false)
    private String code;
    
    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<Ticket> tickets = new java.util.ArrayList<>();
    
    public enum SlotType {
        CAR, BIKE, EV
    }
    
    public enum SlotStatus {
        FREE, OCCUPIED
    }
}
