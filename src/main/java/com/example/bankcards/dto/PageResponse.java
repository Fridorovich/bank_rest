package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * DTO для постраничного ответа
 */
@Data
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean first;
    private boolean last;
}
