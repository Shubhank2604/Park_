package com.park.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLotRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Address is required")
    private String address;
}
