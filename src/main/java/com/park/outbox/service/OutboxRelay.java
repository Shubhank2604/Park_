package com.park.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.outbox.entity.OutboxEvent;
import com.park.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OutboxRelay {
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.max-attempts:8}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        Instant now = Instant.now();
        for (OutboxEvent event : repository
                .findTop50ByPublishedAtIsNullAndDeadLetteredAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(now)) {
            try {
                Class<?> eventClass = Class.forName(event.getEventType());
                Object payload = objectMapper.readValue(event.getPayload(), eventClass);
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), payload).get(5, TimeUnit.SECONDS);
                event.markPublished(Instant.now());
            } catch (Exception exception) {
                long delaySeconds = Math.min(60, 1L << Math.min(event.getAttempts(), 6));
                String error = exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage();
                if (event.getAttempts() + 1 >= maxAttempts) {
                    event.markDeadLettered(Instant.now(), error.substring(0, Math.min(error.length(), 1000)));
                } else {
                    event.markFailed(Instant.now().plus(Duration.ofSeconds(delaySeconds)),
                            error.substring(0, Math.min(error.length(), 1000)));
                }
            }
        }
    }
}
