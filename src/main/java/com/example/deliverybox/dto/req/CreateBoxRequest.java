package com.example.deliverybox.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBoxRequest {

    @Schema(description = "Human-readable label for the box", example = "Box-A1")
    @NotBlank(message = "txref is required")
    @Size(max = 20, message = "txref must be at most 20 characters")
    private String txref;

    @Schema(description = "Maximum weight capacity of the box in grams", example = "100")
    @NotNull(message = "weightLimit is required")
    @Min(value = 1, message = "weightLimit must be greater than 0")
    @Max(value = 500, message = "weightLimit must not exceed 500gr")
    private Integer weightLimit;

    @Min(value = 0, message = "batteryCapacity must be between 0 and 100")
    @Max(value = 100, message = "batteryCapacity must be between 0 and 100")
    private Integer batteryCapacity;
}
