package com.park.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.common.events.SlotUpdated;
import com.park.outbox.entity.OutboxEvent;
import com.park.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxServiceTest {
    @Test
    void recordsTopicKeyTypeAndJsonPayload() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        OutboxService service = new OutboxService(repository, new ObjectMapper().findAndRegisterModules());

        service.record("slot.updated", "42", new SlotUpdated(42L, 7L, "CAR", "FREE"));

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertThat(saved.getTopic()).isEqualTo("slot.updated");
        assertThat(saved.getEventKey()).isEqualTo("42");
        assertThat(saved.getEventType()).isEqualTo(SlotUpdated.class.getName());
        assertThat(saved.getPayload()).contains("\"status\":\"FREE\"");
        assertThat(saved.getPublishedAt()).isNull();
    }
}
