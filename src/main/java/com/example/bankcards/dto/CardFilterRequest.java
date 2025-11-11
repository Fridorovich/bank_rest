package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

/**
 * DTO для фильтрации карт с пагинацией
 */
@Data
@Builder
@AllArgsConstructor
public class CardFilterRequest {
    @Schema(
            description = "Статус карты",
            example = "ACTIVE",
            allowableValues = {"ACTIVE", "BLOCKED", "EXPIRED"}
    )
    private CardStatus status;

    @Schema(
            description = "Поиск по последним 4 цифрам номера карты",
            example = "1234"
    )
    private String searchTerm;

    @Schema(
            description = "Номер страницы (начинается с 0)",
            example = "0"
    )
    private Integer page;

    @Schema(
            description = "Размер страницы",
            example = "10"
    )
    private Integer size;

    @Schema(
            description = "Поле для сортировки",
            example = "balance"
    )
    private String sortBy;

    @Schema(
            description = "Направление сортировки",
            example = "desc",
            allowableValues = {"asc", "desc"}
    )
    private String sortDirection;
    public CardFilterRequest() {
        this.page = 0;
        this.size = 10;
        this.sortBy = "id";
        this.sortDirection = "desc";
    }

    public Sort.Direction getSortDirection() {
        if (sortDirection == null) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.fromString(sortDirection.toUpperCase());
    }
}