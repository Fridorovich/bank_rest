package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/user/cards")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
public class CardUserController {
    private final CardService cardService;

    private Long getCurrentUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CardDto> getUserCard(@PathVariable Long cardId,
                                               Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        CardDto card = cardService.getUserCardById(cardId, userId);
        return ResponseEntity.ok(card);
    }

    @PostMapping("/filter")
    public ResponseEntity<PageResponse<CardDto>> getUserCardsFiltered(
            @Valid @RequestBody CardFilterRequest filter,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        PageResponse<CardDto> cards = cardService.getUserCardsWithFilter(userId, filter);
        return ResponseEntity.ok(cards);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transferBetweenCards(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        TransferResponse transaction = cardService.transferBetweenOwnCards(request, userId);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/{cardId}/block-request")
    public ResponseEntity<String> requestCardBlock(
            @PathVariable Long cardId,
            @Valid @RequestBody BlockCardRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        cardService.requestCardBlock(cardId, userId, request.getReason());
        return ResponseEntity.ok("Block request submitted successfully. Administrator will review it.");
    }


    @GetMapping("/{cardId}/balance")
    public ResponseEntity<BigDecimal> getCardBalance(
            @PathVariable Long cardId,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        BigDecimal balance = cardService.getCardBalance(cardId, userId);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/transfer-history")
    public ResponseEntity<PageResponse<TransferResponse>> getTransferHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        PageResponse<TransferResponse> history = cardService.getTransferHistory(userId, page, size);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/active")
    public ResponseEntity<PageResponse<CardDto>> getActiveCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
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

    @GetMapping
    public ResponseEntity<PageResponse<CardDto>> getUserCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        CardFilterRequest filter = CardFilterRequest.builder()
                .page(page)
                .size(size)
                .build();
        return getUserCardsFiltered(filter, authentication);
    }
}