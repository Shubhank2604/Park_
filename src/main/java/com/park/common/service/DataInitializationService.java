package com.park.common.service;

import com.park.auth.entity.User;
import com.park.auth.repository.UserRepository;
import com.park.auth.service.AuthService;
import com.park.billing.entity.TariffRule;
import com.park.billing.repository.TariffRuleRepository;
import com.park.lot.entity.Level;
import com.park.lot.entity.ParkingLot;
import com.park.lot.entity.Slot;
import com.park.lot.repository.ParkingLotRepository;
import com.park.lot.repository.LevelRepository;
import com.park.lot.repository.SlotRepository;
import com.park.lot.service.SlotCache;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataInitializationService implements CommandLineRunner {
    
    private final AuthService authService;
    private final UserRepository userRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final LevelRepository levelRepository;
    private final SlotRepository slotRepository;
    private final TariffRuleRepository tariffRuleRepository;
    private final SlotCache slotCache;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Initialize default users if they don't exist
        if (userRepository.count() == 0) {
            authService.register("admin", "admin123", User.UserRole.ADMIN);
            authService.register("attendant1", "attendant123", User.UserRole.ATTENDANT);
            authService.register("user1", "user123", User.UserRole.USER);
        }
        
        // Initialize sample parking lot if it doesn't exist
        if (parkingLotRepository.count() == 0) {
            ParkingLot lot = new ParkingLot();
            lot.setName("Downtown Parking");
            lot.setAddress("123 Main St, Downtown");
            lot = parkingLotRepository.save(lot);
            
            // Create levels
            final Level level1 = levelRepository.save(createLevel(lot, 1));
            final Level level2 = levelRepository.save(createLevel(lot, 2));
            
            // Create slots for level 1 (CAR slots)
            List<String> carCodes = List.of("A1", "A2", "A3", "A4", "A5");
            List<Slot> carSlots = carCodes.stream()
                    .map(code -> {
                        Slot slot = new Slot();
                        slot.setLevel(level1);
                        slot.setType(Slot.SlotType.CAR);
                        slot.setStatus(Slot.SlotStatus.FREE);
                        slot.setCode(code);
                        return slot;
                    })
                    .toList();
            carSlots = slotRepository.saveAll(carSlots);
            
            // Create slots for level 2 (BIKE slots)
            List<String> bikeCodes = List.of("B1", "B2", "B3", "B4", "B5");
            List<Slot> bikeSlots = bikeCodes.stream()
                    .map(code -> {
                        Slot slot = new Slot();
                        slot.setLevel(level2);
                        slot.setType(Slot.SlotType.BIKE);
                        slot.setStatus(Slot.SlotStatus.FREE);
                        slot.setCode(code);
                        return slot;
                    })
                    .toList();
            bikeSlots = slotRepository.saveAll(bikeSlots);
            
            // Initialize Redis cache
            for (Slot slot : carSlots) {
                slotCache.addFreeSlot(lot.getId(), slot.getId(), "CAR");
            }
            for (Slot slot : bikeSlots) {
                slotCache.addFreeSlot(lot.getId(), slot.getId(), "BIKE");
            }
            
            // Create default tariff rule
            TariffRule tariffRule = new TariffRule();
            tariffRule.setLotId(lot.getId());
            tariffRule.setBaseMinutes(60);
            tariffRule.setBaseCents(500L); // $5.00
            tariffRule.setStepMinutes(30);
            tariffRule.setStepCents(200L); // $2.00
            tariffRule.setEvSurchargeCents(100L); // $1.00
            tariffRuleRepository.save(tariffRule);
        }
    }
    
    private Level createLevel(ParkingLot lot, int levelNo) {
        Level level = new Level();
        level.setParkingLot(lot);
        level.setLevelNo(levelNo);
        return level;
    }
}
