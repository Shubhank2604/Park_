package com.park.lot.service;

import com.park.lot.entity.Level;
import com.park.lot.entity.ParkingLot;
import com.park.lot.entity.Slot;
import com.park.lot.repository.ParkingLotRepository;
import com.park.lot.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityReconcilerTest {

    @Mock private ParkingLotRepository parkingLotRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private SlotCache slotCache;

    private AvailabilityReconciler reconciler;

    @BeforeEach
    void setUp() {
        reconciler = new AvailabilityReconciler(parkingLotRepository, slotRepository, slotCache);
    }

    @Test
    void replacesEveryTypeSoStaleAndEmptyCacheEntriesAreCleared() {
        ParkingLot lot = new ParkingLot();
        lot.setId(7L);
        Level level = new Level();
        level.setParkingLot(lot);

        Slot freeCar = slot(11L, level, Slot.SlotType.CAR, Slot.SlotStatus.FREE);
        Slot occupiedCar = slot(12L, level, Slot.SlotType.CAR, Slot.SlotStatus.OCCUPIED);
        Slot freeBike = slot(13L, level, Slot.SlotType.BIKE, Slot.SlotStatus.FREE);
        when(parkingLotRepository.findAll()).thenReturn(List.of(lot));
        when(slotRepository.findAllWithLevelAndParkingLot())
                .thenReturn(List.of(freeCar, occupiedCar, freeBike));

        reconciler.reconcile();

        verify(slotCache).replaceFreeSlots(7L, "CAR", List.of(11L));
        verify(slotCache).replaceFreeSlots(7L, "BIKE", List.of(13L));
        verify(slotCache).replaceFreeSlots(7L, "EV", List.of());
    }

    private Slot slot(Long id, Level level, Slot.SlotType type, Slot.SlotStatus status) {
        Slot slot = new Slot();
        slot.setId(id);
        slot.setLevel(level);
        slot.setType(type);
        slot.setStatus(status);
        return slot;
    }
}
