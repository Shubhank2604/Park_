package com.park.lot.service;

import com.park.lot.entity.Level;
import com.park.lot.entity.ParkingLot;
import com.park.lot.entity.Slot;
import com.park.lot.repository.LevelRepository;
import com.park.lot.repository.ParkingLotRepository;
import com.park.lot.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingLotService {
    
    private final ParkingLotRepository parkingLotRepository;
    private final LevelRepository levelRepository;
    private final SlotRepository slotRepository;
    private final SlotCache slotCache;
    
    public ParkingLot createLot(String name, String address) {
        ParkingLot lot = new ParkingLot();
        lot.setName(name);
        lot.setAddress(address);
        return parkingLotRepository.save(lot);
    }
    
    public ParkingLot getLot(Long id) {
        return parkingLotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking lot not found"));
    }
    
    public List<ParkingLot> getAllLots() {
        return parkingLotRepository.findAll();
    }
    
    public Level createLevel(Long lotId, Integer levelNo) {
        ParkingLot lot = getLot(lotId);
        
        Level level = new Level();
        level.setParkingLot(lot);
        level.setLevelNo(levelNo);
        
        return levelRepository.save(level);
    }
    
    @Transactional
    public List<Slot> createSlots(Long levelId, Slot.SlotType type, List<String> codes) {
        Level level = levelRepository.findById(levelId)
                .orElseThrow(() -> new RuntimeException("Level not found"));
        
        List<Slot> slots = codes.stream()
                .map(code -> {
                    Slot slot = new Slot();
                    slot.setLevel(level);
                    slot.setType(type);
                    slot.setStatus(Slot.SlotStatus.FREE);
                    slot.setCode(code);
                    return slot;
                })
                .toList();
        
        List<Slot> savedSlots = slotRepository.saveAll(slots);
        
        // Initialize Redis cache for new slots
        for (Slot slot : savedSlots) {
            slotCache.addFreeSlot(slot.getLevel().getParkingLot().getId(), slot.getId(), type.name());
        }
        
        return savedSlots;
    }
    
    public long getAvailability(Long lotId, String type) {
        return slotCache.getFreeSlotCount(lotId, type);
    }
}
