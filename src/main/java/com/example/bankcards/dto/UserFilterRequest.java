package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * DTO для фильтрации пользователей
 */
@Data
@Builder
@AllArgsConstructor
public class UserFilterRequest {
    private String username;
    private String role;
    private Boolean active;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;

    public UserFilterRequest() {
        this.page = 0;
        this.size = 10;
        this.sortBy = "id";
        this.sortDirection = "desc";
    }

    public String getSortDirection() {
        if (sortDirection == null) {
            return "desc";
        }
        return sortDirection.toLowerCase().equals("asc") ? "asc" : "desc";
    }
}