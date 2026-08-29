package com.park.outbox.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_pending", columnList = "published_at,next_attempt_at,created_at"))
@Getter
@NoArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "dead_lettered_at")
    private Instant deadLetteredAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    public OutboxEvent(String topic, String eventKey, String eventType, String payload, Instant now) {
        this.topic = topic;
        this.eventKey = eventKey;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = now;
        this.nextAttemptAt = now;
    }

    public void markPublished(Instant now) {
        this.publishedAt = now;
        this.lastError = null;
    }

    public void markFailed(Instant nextAttemptAt, String error) {
        this.attempts += 1;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = error;
    }

    public void markDeadLettered(Instant now, String error) {
        this.attempts += 1;
        this.deadLetteredAt = now;
        this.lastError = error;
    }
}
