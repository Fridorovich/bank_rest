package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.UpdateCardRequest;
import com.example.bankcards.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cards")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CardAdminController {
    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardDto> createCard(@RequestBody CreateCardRequest request) {
        CardDto card = cardService.createCard(request);
        return ResponseEntity.ok(card);
    }

    @GetMapping
    public ResponseEntity<List<CardDto>> getAllCards() {
        List<CardDto> cards = cardService.getAllCards();
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CardDto> getCard(@PathVariable Long cardId) {
        CardDto card = cardService.getCardById(cardId);
        return ResponseEntity.ok(card);
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<CardDto> updateCard(@PathVariable Long cardId,
                                              @RequestBody UpdateCardRequest request) {
        CardDto card = cardService.updateCard(cardId, request);
        return ResponseEntity.ok(card);
    }

    @PostMapping("/{cardId}/block")
    public ResponseEntity<CardDto> blockCard(@PathVariable Long cardId) {
        CardDto card = cardService.blockCard(cardId);
        return ResponseEntity.ok(card);
    }

    @PostMapping("/{cardId}/activate")
    public ResponseEntity<CardDto> activateCard(@PathVariable Long cardId) {
        CardDto card = cardService.activateCard(cardId);
        return ResponseEntity.ok(card);
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CardDto>> getCardsByStatus(@PathVariable String status) {
        List<CardDto> cards = cardService.getCardsByStatus(status);
        return ResponseEntity.ok(cards);
    }
}