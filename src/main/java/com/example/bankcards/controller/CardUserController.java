package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public ResponseEntity<List<CardDto>> getUserCards(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<CardDto> cards = cardService.getUserCards(userId);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CardDto> getUserCard(@PathVariable Long cardId,
                                               Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        CardDto card = cardService.getUserCardById(cardId, userId);
        return ResponseEntity.ok(card);
    }

    @GetMapping("/active")
    public ResponseEntity<List<CardDto>> getUserActiveCards(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<CardDto> cards = cardService.getUserActiveCards(userId);
        return ResponseEntity.ok(cards);
    }
}