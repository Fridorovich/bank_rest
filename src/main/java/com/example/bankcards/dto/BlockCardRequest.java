package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Запрос на блокировку карты")
public class BlockCardRequest {

    @Schema(
            description = "Причина блокировки карты",
            example = "Карта утеряна",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String reason;
}