package com.park.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.common.events.SlotUpdated;
import com.park.outbox.entity.OutboxEvent;
import com.park.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxRelayTest {
    @Test
    void marksEventPublishedOnlyAfterKafkaAcknowledgesIt() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        OutboxEvent event = new OutboxEvent(
                "slot.updated", "42", SlotUpdated.class.getName(),
                mapper.writeValueAsString(new SlotUpdated(42L, 7L, "CAR", "FREE")), Instant.now());
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        when(repository.findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(event));

        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, Object>> acknowledged =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("slot.updated"), eq("42"), any())).thenReturn(acknowledged);

        new OutboxRelay(repository, kafkaTemplate, mapper).publishPending();

        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttempts()).isZero();
    }

    @Test
    void schedulesRetryWhenKafkaFails() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        OutboxEvent event = new OutboxEvent(
                "slot.updated", "42", SlotUpdated.class.getName(),
                mapper.writeValueAsString(new SlotUpdated(42L, 7L, "CAR", "FREE")), Instant.now());
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        when(repository.findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(event));

        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send(eq("slot.updated"), eq("42"), any())).thenReturn(failed);

        new OutboxRelay(repository, kafkaTemplate, mapper).publishPending();

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(Instant.now());
    }
}
