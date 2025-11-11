package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Запрос для фильтрации пользователей с пагинацией")
public class UserFilterRequest {
    @Schema(
            description = "Имя пользователя (поиск по частичному совпадению)",
            example = "john"
    )
    private String username;

    @Schema(
            description = "Роль пользователя",
            example = "USER",
            allowableValues = {"USER", "ADMIN"}
    )
    private String role;

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
            example = "username"
    )
    private String sortBy;

    @Schema(
            description = "Направление сортировки",
            example = "asc",
            allowableValues = {"asc", "desc"}
    )
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