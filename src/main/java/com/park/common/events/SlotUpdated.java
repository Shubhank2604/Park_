package com.park.common.events;

public record SlotUpdated(
    Long slotId,
    Long lotId,
    String type,
    String status
) {}
