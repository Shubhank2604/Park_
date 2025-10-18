package com.park.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExitRequest {
    @NotBlank(message = "Either ticket ID or plate number is required")
    private String identifier; // Can be ticketId or plateNo
}
