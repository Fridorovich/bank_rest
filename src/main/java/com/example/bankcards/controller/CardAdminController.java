package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.UpdateCardRequest;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для управления банковскими картами администратором
 * Предоставляет полный CRUD функционал для работы с картами
 */
@RestController
@RequestMapping("/api/admin/cards")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Card Admin Controller", description = "API для управления банковскими картами администратором")
public class CardAdminController {
    private final CardService cardService;

    /**
     * Создание новой банковской карты
     */
    @Operation(
            summary = "Создание новой карты",
            description = "Создает новую банковскую карту для указанного пользователя. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно создана",
                    content = @Content(schema = @Schema(implementation = CardDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные карты или номер карты уже существует"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @PostMapping
    public ResponseEntity<CardDto> createCard(
            @Parameter(description = "Данные для создания карты", required = true)
            @RequestBody CreateCardRequest request) {
        CardDto card = cardService.createCard(request);
        return ResponseEntity.ok(card);
    }

    /**
     * Получение всех карт в системе
     */
    @Operation(
            summary = "Получение всех карт",
            description = "Возвращает список всех банковских карт в системе. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список карт успешно получен"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @GetMapping
    public ResponseEntity<List<CardDto>> getAllCards() {
        List<CardDto> cards = cardService.getAllCards();
        return ResponseEntity.ok(cards);
    }

    /**
     * Получение карты по идентификатору
     */
    @Operation(
            summary = "Получение карты по ID",
            description = "Возвращает детальную информацию о карте по её идентификатору. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно найдена",
                    content = @Content(schema = @Schema(implementation = CardDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта с указанным ID не найдена"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @GetMapping("/{cardId}")
    public ResponseEntity<CardDto> getCard(
            @Parameter(description = "Идентификатор карты", required = true, example = "1")
            @PathVariable Long cardId) {
        CardDto card = cardService.getCardById(cardId);
        return ResponseEntity.ok(card);
    }

    /**
     * Обновление данных карты
     */
    @Operation(
            summary = "Обновление данных карты",
            description = "Обновляет баланс и/или статус карты. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Данные карты успешно обновлены",
                    content = @Content(schema = @Schema(implementation = CardDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные для обновления"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта с указанным ID не найдена"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @PutMapping("/{cardId}")
    public ResponseEntity<CardDto> updateCard(
            @Parameter(description = "Идентификатор карты", required = true, example = "1")
            @PathVariable Long cardId,
            @Parameter(description = "Данные для обновления карты", required = true)
            @RequestBody UpdateCardRequest request) {
        CardDto card = cardService.updateCard(cardId, request);
        return ResponseEntity.ok(card);
    }

    /**
     * Блокировка карты
     */
    @Operation(
            summary = "Блокировка карты",
            description = "Блокирует карту, предотвращая любые операции по ней. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно заблокирована",
                    content = @Content(schema = @Schema(implementation = CardDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта с указанным ID не найдена"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @PostMapping("/{cardId}/block")
    public ResponseEntity<CardDto> blockCard(
            @Parameter(description = "Идентификатор карты", required = true, example = "1")
            @PathVariable Long cardId) {
        CardDto card = cardService.blockCard(cardId);
        return ResponseEntity.ok(card);
    }

    /**
     * Активация карты
     */
    @Operation(
            summary = "Активация карты",
            description = "Активирует ранее заблокированную карту. Проверяет срок действия перед активацией. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно активирована",
                    content = @Content(schema = @Schema(implementation = CardDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невозможно активировать карту с истекшим сроком действия"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта с указанным ID не найдена"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @PostMapping("/{cardId}/activate")
    public ResponseEntity<CardDto> activateCard(
            @Parameter(description = "Идентификатор карты", required = true, example = "1")
            @PathVariable Long cardId) {
        CardDto card = cardService.activateCard(cardId);
        return ResponseEntity.ok(card);
    }

    /**
     * Удаление карты
     */
    @Operation(
            summary = "Удаление карты",
            description = "Удаляет карту из системы. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Карта успешно удалена"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта с указанным ID не найдена"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @Parameter(description = "Идентификатор карты", required = true, example = "1")
            @PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получение карт по статусу
     */
    @Operation(
            summary = "Получение карт по статусу",
            description = "Возвращает список карт с указанным статусом. Требуются права администратора."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список карт успешно получен"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный статус карты"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Отсутствуют права администратора"
            )
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<CardDto>> getCardsByStatus(
            @Parameter(
                    description = "Статус карты",
                    required = true,
                    example = "ACTIVE",
                    schema = @Schema(allowableValues = {"ACTIVE", "BLOCKED", "EXPIRED"})
            )
            @PathVariable String status) {
        List<CardDto> cards = cardService.getCardsByStatus(status);
        return ResponseEntity.ok(cards);
    }
}