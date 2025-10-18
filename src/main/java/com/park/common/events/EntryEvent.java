package com.park.common.events;

import java.time.Instant;

public record EntryEvent(
    Long ticketId,
    Long lotId,
    Long slotId,
    String plate,
    String type,
    Instant entryTime
) {}
