package com.park.common.events;

public record InvoiceCreated(
    Long invoiceId,
    Long ticketId,
    long amountCents,
    String currency
) {}
