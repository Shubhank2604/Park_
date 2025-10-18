package com.park.common.events;

public record PaymentCompleted(
    Long invoiceId,
    Long ticketId,
    long amountCents,
    String currency
) {}
