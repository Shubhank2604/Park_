package com.park.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EntryRequest {
    @NotNull(message = "Lot ID is required")
    private Long lotId;
    
    @NotBlank(message = "Plate number is required")
    private String plateNo;
    
    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;
}
