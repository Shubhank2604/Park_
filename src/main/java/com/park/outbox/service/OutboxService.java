package com.park.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.outbox.entity.OutboxEvent;
import com.park.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public void record(String topic, String key, Object event) {
        try {
            repository.save(new OutboxEvent(
                    topic, key, event.getClass().getName(), objectMapper.writeValueAsString(event), Instant.now()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize outbox event", exception);
        }
    }
}
