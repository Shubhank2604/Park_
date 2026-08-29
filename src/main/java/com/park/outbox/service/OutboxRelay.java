package com.park.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.outbox.entity.OutboxEvent;
import com.park.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OutboxRelay {
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        Instant now = Instant.now();
        for (OutboxEvent event : repository
                .findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(now)) {
            try {
                Class<?> eventClass = Class.forName(event.getEventType());
                Object payload = objectMapper.readValue(event.getPayload(), eventClass);
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), payload).get(5, TimeUnit.SECONDS);
                event.markPublished(Instant.now());
            } catch (Exception exception) {
                long delaySeconds = Math.min(60, 1L << Math.min(event.getAttempts(), 6));
                event.markFailed(Instant.now().plus(Duration.ofSeconds(delaySeconds)));
            }
        }
    }
}
