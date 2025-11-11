package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Контроллер для операций пользователя с банковскими картами
 * Предоставляет функционал для просмотра карт, переводов и управления картами пользователя
 */
@RestController
@RequestMapping("/api/user/cards")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
@Tag(name = "Card User Controller", description = "API для операций пользователя с банковскими картами")
public class CardUserController {
    private final CardService cardService;

    private Long getCurrentUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }

    /**
     * Получение конкретной карты пользователя
     */
    @Operation(
            summary = "Получение карты по ID",
            description = "Возвращает детальную информацию о конкретной карте пользователя. Пользователь может получить только свои карты."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно найдена",
                    content = @Content(schema = @Schema(implementation = CardDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта не найдена или не принадлежит пользователю"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права пользователя"
            )
    })
    @GetMapping("/{cardId}")
    public ResponseEntity<CardDto> getUserCard(
            @Parameter(description = "Идентификатор карты", required = true, example = "1")
            @PathVariable Long cardId,
            @Parameter(hidden = true)
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        CardDto card = cardService.getUserCardById(cardId, userId);
        return ResponseEntity.ok(card);
    }

    /**
     * Получение отфильтрованного списка карт с пагинацией
     */
    @Operation(
            summary = "Фильтрация карт с пагинацией",
            description = "Возвращает отфильтрованный список карт пользователя с поддержкой пагинации, сортировки и поиска по номеру карты."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список карт успешно получен"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные параметры фильтрации"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права пользователя"
            )
    })
    @PostMapping("/filter")
    public ResponseEntity<PageResponse<CardDto>> getUserCardsFiltered(
            @Parameter(description = "Параметры фильтрации и пагинации", required = true)
            @Valid @RequestBody CardFilterRequest filter,
            @Parameter(hidden = true)
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        PageResponse<CardDto> cards = cardService.getUserCardsWithFilter(userId, filter);
        return ResponseEntity.ok(cards);
    }

    /**
     * Перевод средств между картами пользователя
     */
    @Operation(
            summary = "Перевод между картами",
            description = "Выполняет перевод средств между картами одного пользователя. Проверяет достаточность средств и статус карт."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Перевод успешно выполнен",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Недостаточно средств, карты заблокированы или другие ошибки валидации"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права пользователя"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Одна из карт не найдена или не принадлежит пользователю"
            )
    })
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transferBetweenCards(
            @Parameter(description = "Данные для перевода", required = true)
            @Valid @RequestBody TransferRequest request,
            @Parameter(hidden = true)
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        TransferResponse transaction = cardService.transferBetweenOwnCards(request, userId);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Запрос на блокировку карты
     */
    @Operation(
            summary = "Запрос блокировки карты",
            description = "Отправляет запрос администратору на блокировку карты. Администратор рассмотрит запрос и выполнит блокировку."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Запрос на блокировку успешно отправлен"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта не найдена или не принадлежит пользователю"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права пользователя"
            )
    })
    @PostMapping("/{cardId}/block-request")
    public ResponseEntity<String> requestCardBlock(
            @Parameter(description = "Идентификатор карты", required = true, example = "1")
            @PathVariable Long cardId,
            @Parameter(description = "Причина блокировки", required = true)
            @Valid @RequestBody BlockCardRequest request,
            @Parameter(hidden = true)
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        cardService.requestCardBlock(cardId, userId, request.getReason());
        return ResponseEntity.ok("Block request submitted successfully. Administrator will review it.");
    }

    /**
     * Получение баланса карты
     */
    @Operation(
            summary = "Получение баланса карты",
            description = "Возвращает текущий баланс указанной карты пользователя."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Баланс успешно получен"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта не найдена или не принадлежит пользователю"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права пользователя"
            )
    })
    @GetMapping("/{cardId}/balance")
    public ResponseEntity<BigDecimal> getCardBalance(
            @Parameter(description = "Идентификатор карты", required = true, example = "1")
            @PathVariable Long cardId,
            @Parameter(hidden = true)
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        BigDecimal balance = cardService.getCardBalance(cardId, userId);
        return ResponseEntity.ok(balance);
    }

    /**
     * Получение истории переводов
     */
    @Operation(
            summary = "История переводов",
            description = "Возвращает историю переводов пользователя с поддержкой пагинации."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "История переводов успешно получена"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права пользователя"
            )
    })
    @GetMapping("/transfer-history")
    public ResponseEntity<PageResponse<TransferResponse>> getTransferHistory(
            @Parameter(description = "Номер страницы", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true)
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        PageResponse<TransferResponse> history = cardService.getTransferHistory(userId, page, size);
        return ResponseEntity.ok(history);
    }

    /**
     * Получение активных карт
     */
    @Operation(
            summary = "Получение активных карт",
            description = "Возвращает список активных карт пользователя с поддержкой пагинации."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список активных карт успешно получен"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права пользователя"
            )
    })
    @GetMapping("/active")
    public ResponseEntity<PageResponse<CardDto>> getActiveCards(
            @Parameter(description = "Номер страницы", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true)
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        CardFilterRequest filter = CardFilterRequest.builder()
                .status(CardStatus.ACTIVE)
                .page(page)
                .size(size)
                .build();

        PageResponse<CardDto> cards = cardService.getUserCardsWithFilter(userId, filter);
        return ResponseEntity.ok(cards);
    }

    /**
     * Получение всех карт пользователя
     */
    @Operation(
            summary = "Получение всех карт",
            description = "Возвращает все карты пользователя с поддержкой пагинации."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список карт успешно получен"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права пользователя"
            )
    })
    @GetMapping
    public ResponseEntity<PageResponse<CardDto>> getUserCards(
            @Parameter(description = "Номер страницы", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true)
            Authentication authentication) {
        CardFilterRequest filter = CardFilterRequest.builder()
                .page(page)
                .size(size)
                .build();
        return getUserCardsFiltered(filter, authentication);
    }
}