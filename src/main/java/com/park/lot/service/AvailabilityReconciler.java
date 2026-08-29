package com.park.lot.service;

import com.park.lot.entity.ParkingLot;
import com.park.lot.entity.Slot;
import com.park.lot.repository.ParkingLotRepository;
import com.park.lot.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityReconciler {

    private final ParkingLotRepository parkingLotRepository;
    private final SlotRepository slotRepository;
    private final SlotCache slotCache;

    @Scheduled(
            fixedDelayString = "${app.availability.reconcile-delay-ms:60000}",
            initialDelayString = "${app.availability.reconcile-initial-delay-ms:5000}"
    )
    @Transactional(readOnly = true)
    public void reconcile() {
        List<Long> lotIds = parkingLotRepository.findAll().stream().map(ParkingLot::getId).toList();
        Map<Long, Map<Slot.SlotType, List<Long>>> freeSlots = new java.util.HashMap<>();

        for (Slot slot : slotRepository.findAllWithLevelAndParkingLot()) {
            if (slot.getStatus() != Slot.SlotStatus.FREE) {
                continue;
            }
            long lotId = slot.getLevel().getParkingLot().getId();
            freeSlots.computeIfAbsent(lotId, ignored -> new EnumMap<>(Slot.SlotType.class))
                    .computeIfAbsent(slot.getType(), ignored -> new java.util.ArrayList<>())
                    .add(slot.getId());
        }

        for (long lotId : lotIds) {
            for (Slot.SlotType type : Slot.SlotType.values()) {
                Map<Slot.SlotType, List<Long>> slotsByType = freeSlots.get(lotId);
                List<Long> ids = slotsByType == null
                        ? List.of()
                        : slotsByType.getOrDefault(type, List.of());
                slotCache.replaceFreeSlots(lotId, type.name(), ids);
            }
        }

        log.debug("Reconciled availability cache for {} parking lots", lotIds.size());
    }
}
