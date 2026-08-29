package com.park.lot.repository;

import com.park.lot.entity.Level;
import com.park.lot.entity.ParkingLot;
import com.park.lot.entity.Slot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class SlotRepositoryTest {

    @Autowired private ParkingLotRepository parkingLotRepository;
    @Autowired private LevelRepository levelRepository;
    @Autowired private SlotRepository slotRepository;

    @Test
    void allocationQueryReturnsFirstCompatibleFreeSlotOnly() {
        ParkingLot lot = new ParkingLot();
        lot.setName("Downtown");
        lot.setAddress("1 Main St");
        lot = parkingLotRepository.save(lot);

        Level level = new Level();
        level.setParkingLot(lot);
        level.setLevelNo(1);
        level = levelRepository.save(level);

        saveSlot(level, "A-02", Slot.SlotType.CAR, Slot.SlotStatus.OCCUPIED);
        Slot expected = saveSlot(level, "A-01", Slot.SlotType.CAR, Slot.SlotStatus.FREE);
        saveSlot(level, "B-01", Slot.SlotType.BIKE, Slot.SlotStatus.FREE);

        assertThat(slotRepository
                .findFirstByLevelParkingLotIdAndTypeAndStatusOrderByLevelLevelNoAscIdAsc(
                        lot.getId(), Slot.SlotType.CAR, Slot.SlotStatus.FREE))
                .contains(expected);
    }

    private Slot saveSlot(Level level, String code, Slot.SlotType type, Slot.SlotStatus status) {
        Slot slot = new Slot();
        slot.setLevel(level);
        slot.setCode(code);
        slot.setType(type);
        slot.setStatus(status);
        return slotRepository.save(slot);
    }
}
