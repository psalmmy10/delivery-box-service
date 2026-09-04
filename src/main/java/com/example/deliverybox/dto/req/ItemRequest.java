package com.example.deliverybox.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemRequest {

    @NotBlank(message = "name is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "name may only contain letters, numbers, hyphen '-' and underscore '_'")
    private String name;

    @NotNull(message = "weight is required")
    @Min(value = 1, message = "weight must be greater than 0")
    private Integer weight;

    @NotBlank(message = "code is required")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "code may only contain upper case letters, numbers and underscore '_'")
    private String code;

}
