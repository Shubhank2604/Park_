package com.park.common.service;

import com.park.auth.entity.User;
import com.park.auth.repository.UserRepository;
import com.park.auth.service.AuthService;
import com.park.billing.entity.TariffRule;
import com.park.billing.repository.TariffRuleRepository;
import com.park.lot.entity.Level;
import com.park.lot.entity.ParkingLot;
import com.park.lot.entity.Slot;
import com.park.lot.repository.LevelRepository;
import com.park.lot.repository.ParkingLotRepository;
import com.park.lot.repository.SlotRepository;
import com.park.lot.service.SlotCache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataInitializationService implements CommandLineRunner {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final LevelRepository levelRepository;
    private final SlotRepository slotRepository;
    private final TariffRuleRepository tariffRuleRepository;
    private final SlotCache slotCache;

    @Value("${app.seed.admin-username}")
    private String adminUsername;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.attendant-username}")
    private String attendantUsername;

    @Value("${app.seed.attendant-password}")
    private String attendantPassword;

    @Value("${app.seed.user-username}")
    private String userUsername;

    @Value("${app.seed.user-password}")
    private String userPassword;

    @Override
    @Transactional
    public void run(String... args) {
        validateSeedCredentials();

        if (userRepository.count() == 0) {
            authService.register(adminUsername, adminPassword, User.UserRole.ADMIN);
            authService.register(attendantUsername, attendantPassword, User.UserRole.ATTENDANT);
            authService.register(userUsername, userPassword, User.UserRole.USER);
        }

        if (parkingLotRepository.count() == 0) {
            ParkingLot lot = new ParkingLot();
            lot.setName("Downtown Parking");
            lot.setAddress("123 Main St, Downtown");
            lot = parkingLotRepository.save(lot);

            final Level level1 = levelRepository.save(createLevel(lot, 1));
            final Level level2 = levelRepository.save(createLevel(lot, 2));

            List<Slot> carSlots = createSlots(level1, Slot.SlotType.CAR, List.of("A1", "A2", "A3", "A4", "A5"));
            List<Slot> bikeSlots = createSlots(level2, Slot.SlotType.BIKE, List.of("B1", "B2", "B3", "B4", "B5"));

            for (Slot slot : carSlots) {
                slotCache.addFreeSlot(lot.getId(), slot.getId(), "CAR");
            }
            for (Slot slot : bikeSlots) {
                slotCache.addFreeSlot(lot.getId(), slot.getId(), "BIKE");
            }

            TariffRule tariffRule = new TariffRule();
            tariffRule.setLotId(lot.getId());
            tariffRule.setBaseMinutes(60);
            tariffRule.setBaseCents(500L);
            tariffRule.setStepMinutes(30);
            tariffRule.setStepCents(200L);
            tariffRule.setEvSurchargeCents(100L);
            tariffRuleRepository.save(tariffRule);
        }
    }

    private void validateSeedCredentials() {
        if (adminPassword.isBlank() || attendantPassword.isBlank() || userPassword.isBlank()) {
            throw new IllegalStateException(
                    "Demo seeding is enabled, but one or more SEED_*_PASSWORD values are missing"
            );
        }
    }

    private List<Slot> createSlots(Level level, Slot.SlotType type, List<String> codes) {
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
        return slotRepository.saveAll(slots);
    }

    private Level createLevel(ParkingLot lot, int levelNo) {
        Level level = new Level();
        level.setParkingLot(lot);
        level.setLevelNo(levelNo);
        return level;
    }
}
