package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
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
    private CardStatus status;
    private String searchTerm;
    private Integer page;
    private Integer size;
    private String sortBy;
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