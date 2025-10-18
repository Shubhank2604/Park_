package com.park.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLevelRequest {
    @NotNull(message = "Level number is required")
    private Integer levelNo;
}
