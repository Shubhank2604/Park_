package com.park.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateSlotsRequest {
    @NotNull(message = "Slot type is required")
    private String type;
    
    @NotNull(message = "Slot codes are required")
    private List<String> codes;
}
