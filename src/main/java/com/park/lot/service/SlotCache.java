package com.park.lot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SlotCache {
    
    private final StringRedisTemplate redisTemplate;
    
    public void markOccupied(long lotId, long slotId, String type) {
        String slotKey = "slot:" + slotId;
        String freeCounterKey = "lot:" + lotId + ":free:" + type;
        String freeSlotsKey = "lot:" + lotId + ":freeSlots:" + type;
        
        // Update slot status
        redisTemplate.opsForHash().putAll(slotKey, Map.of(
            "status", "OCCUPIED",
            "type", type,
            "lotId", String.valueOf(lotId)
        ));
        
        // Decrement free counter
        redisTemplate.opsForValue().decrement(freeCounterKey);
        
        // Remove from free slots set
        redisTemplate.opsForSet().remove(freeSlotsKey, String.valueOf(slotId));
    }
    
    public void markFree(long lotId, long slotId, String type) {
        String slotKey = "slot:" + slotId;
        String freeCounterKey = "lot:" + lotId + ":free:" + type;
        String freeSlotsKey = "lot:" + lotId + ":freeSlots:" + type;
        
        // Update slot status
        redisTemplate.opsForHash().put(slotKey, "status", "FREE");
        
        // Increment free counter
        redisTemplate.opsForValue().increment(freeCounterKey);
        
        // Add to free slots set
        redisTemplate.opsForSet().add(freeSlotsKey, String.valueOf(slotId));
    }
    
    public Optional<Long> popAnyFreeSlot(long lotId, String type) {
        String freeSlotsKey = "lot:" + lotId + ":freeSlots:" + type;
        String member = redisTemplate.opsForSet().pop(freeSlotsKey);
        return member == null ? Optional.empty() : Optional.of(Long.parseLong(member));
    }
    
    public void addFreeSlot(long lotId, long slotId, String type) {
        String freeSlotsKey = "lot:" + lotId + ":freeSlots:" + type;
        String freeCounterKey = "lot:" + lotId + ":free:" + type;
        
        redisTemplate.opsForSet().add(freeSlotsKey, String.valueOf(slotId));
        redisTemplate.opsForValue().increment(freeCounterKey);
    }

    public void replaceFreeSlots(long lotId, String type, Collection<Long> freeSlotIds) {
        String freeSlotsKey = "lot:" + lotId + ":freeSlots:" + type;
        String freeCounterKey = "lot:" + lotId + ":free:" + type;

        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public <K, V> java.util.List<Object> execute(RedisOperations<K, V> operations) {
                StringRedisTemplate template = (StringRedisTemplate) operations;
                template.multi();
                template.delete(freeSlotsKey);
                template.opsForValue().set(freeCounterKey, String.valueOf(freeSlotIds.size()));
                if (!freeSlotIds.isEmpty()) {
                    template.opsForSet().add(
                            freeSlotsKey,
                            freeSlotIds.stream().map(String::valueOf).toArray(String[]::new)
                    );
                }
                return template.exec();
            }
        });
    }
    
    public long getFreeSlotCount(long lotId, String type) {
        String freeCounterKey = "lot:" + lotId + ":free:" + type;
        String count = redisTemplate.opsForValue().get(freeCounterKey);
        return count == null ? 0 : Long.parseLong(count);
    }
    
    public void cacheOpenTicket(String plateNo, Long ticketId) {
        String key = "vehicle:openTicket:" + plateNo;
        redisTemplate.opsForValue().set(key, String.valueOf(ticketId));
    }
    
    public Optional<Long> getOpenTicket(String plateNo) {
        String key = "vehicle:openTicket:" + plateNo;
        String ticketId = redisTemplate.opsForValue().get(key);
        return ticketId == null ? Optional.empty() : Optional.of(Long.parseLong(ticketId));
    }
    
    public void removeOpenTicket(String plateNo) {
        String key = "vehicle:openTicket:" + plateNo;
        redisTemplate.delete(key);
    }
    
    public void cacheTicketSummary(Long ticketId, Map<String, String> summary) {
        String key = "ticket:summary:" + ticketId;
        redisTemplate.opsForHash().putAll(key, summary);
        // Set TTL to 24 hours
        redisTemplate.expire(key, java.time.Duration.ofHours(24));
    }
}
