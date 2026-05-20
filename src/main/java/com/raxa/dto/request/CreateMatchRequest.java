package com.raxa.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateMatchRequest(
        @NotBlank @Size(min = 3, max = 100) String title,
        @NotBlank @Size(max = 255) String location,
        @NotNull @Future LocalDateTime scheduledAt,
        @NotNull @Min(2) @Max(100) Integer maxPlayers
) {
}
