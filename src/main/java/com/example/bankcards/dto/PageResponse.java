package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
@Schema(description = "Ответ с пагинацией")
public class PageResponse<T> {

    @Schema(description = "Содержимое страницы")
    private List<T> content;

    @Schema(description = "Текущая страница", example = "0")
    private int currentPage;

    @Schema(description = "Общее количество страниц", example = "5")
    private int totalPages;

    @Schema(description = "Общее количество элементов", example = "47")
    private long totalElements;

    @Schema(description = "Размер страницы", example = "10")
    private int pageSize;

    @Schema(description = "Является ли первой страницей", example = "true")
    private boolean first;

    @Schema(description = "Является ли последней страницей", example = "false")
    private boolean last;
}