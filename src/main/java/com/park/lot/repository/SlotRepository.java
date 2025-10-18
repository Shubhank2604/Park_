package com.park.lot.repository;

import com.park.lot.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByLevelId(Long levelId);
    
    @Query("SELECT s FROM Slot s WHERE s.level.parkingLot.id = :lotId AND s.type = :type AND s.status = 'FREE' ORDER BY s.level.levelNo, s.id LIMIT 1")
    Optional<Slot> findFirstFreeSlotByLotAndType(@Param("lotId") Long lotId, @Param("type") Slot.SlotType type);
    
    @Query("SELECT COUNT(s) FROM Slot s WHERE s.level.parkingLot.id = :lotId AND s.type = :type AND s.status = 'FREE'")
    long countFreeSlotsByLotAndType(@Param("lotId") Long lotId, @Param("type") Slot.SlotType type);
}
