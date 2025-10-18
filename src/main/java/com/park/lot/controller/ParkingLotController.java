package com.park.lot.controller;

import com.park.common.dto.CreateLevelRequest;
import com.park.common.dto.CreateLotRequest;
import com.park.common.dto.CreateSlotsRequest;
import com.park.lot.entity.Level;
import com.park.lot.entity.ParkingLot;
import com.park.lot.entity.Slot;
import com.park.lot.service.ParkingLotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lots")
@RequiredArgsConstructor
public class ParkingLotController {
    
    private final ParkingLotService parkingLotService;
    
    @PostMapping
    public ResponseEntity<ParkingLot> createLot(@Valid @RequestBody CreateLotRequest request) {
        ParkingLot lot = parkingLotService.createLot(request.getName(), request.getAddress());
        return ResponseEntity.ok(lot);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ParkingLot> getLot(@PathVariable Long id) {
        ParkingLot lot = parkingLotService.getLot(id);
        return ResponseEntity.ok(lot);
    }
    
    @GetMapping
    public ResponseEntity<List<ParkingLot>> getAllLots() {
        List<ParkingLot> lots = parkingLotService.getAllLots();
        return ResponseEntity.ok(lots);
    }
    
    @PostMapping("/{id}/levels")
    public ResponseEntity<Level> createLevel(@PathVariable Long id, @Valid @RequestBody CreateLevelRequest request) {
        Level level = parkingLotService.createLevel(id, request.getLevelNo());
        return ResponseEntity.ok(level);
    }
    
    @PostMapping("/levels/{id}/slots")
    public ResponseEntity<List<Slot>> createSlots(@PathVariable Long id, @Valid @RequestBody CreateSlotsRequest request) {
        Slot.SlotType type = Slot.SlotType.valueOf(request.getType().toUpperCase());
        List<Slot> slots = parkingLotService.createSlots(id, type, request.getCodes());
        return ResponseEntity.ok(slots);
    }
    
    @GetMapping("/{id}/availability")
    public ResponseEntity<Map<String, Long>> getAvailability(@PathVariable Long id, @RequestParam String type) {
        long count = parkingLotService.getAvailability(id, type.toUpperCase());
        return ResponseEntity.ok(Map.of("freeSlots", count));
    }
}
