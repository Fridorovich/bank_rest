package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.UpdateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для управления банковскими картами
 * Реализует CRUD операции с разделением прав доступа между ADMIN и USER
 */
@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    /**
     * Создание новой карты (только для ADMIN)
     * @param request данные для создания карты
     * @return созданная карта в формате DTO
     * @throws RuntimeException если номер карты уже существует или срок действия истек
     */
    @Transactional
    public CardDto createCard(CreateCardRequest request) {
        if (cardRepository.existsByNumber(request.getNumber())) {
            throw new RuntimeException("Card with this number already exists: " + request.getNumber());
        }

        if (request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Expiry date cannot be in the past: " + request.getExpiryDate());
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        Card card = Card.builder()
                .number(request.getNumber())
                .expiryDate(request.getExpiryDate())
                .status(CardStatus.ACTIVE)
                .balance(request.getInitialBalance() != null ?
                        request.getInitialBalance() : BigDecimal.ZERO)
                .user(user)
                .build();

        Card savedCard = cardRepository.save(card);
        return convertToDto(savedCard);
    }

    /**
     * Получение всех карт (только для ADMIN)
     * @return список всех карт в системе
     */
    public List<CardDto> getAllCards() {
        return cardRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение карты по ID (только для ADMIN)
     * @param cardId идентификатор карты
     * @return карта в формате DTO
     * @throws RuntimeException если карта не найдена
     */
    public CardDto getCardById(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + cardId));
        return convertToDto(card);
    }

    /**
     * Получение карт текущего пользователя (для USER)
     * @param userId идентификатор пользователя
     * @return список карт пользователя
     */
    public List<CardDto> getUserCards(Long userId) {
        return cardRepository.findByUserId(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение конкретной карты пользователя (для USER)
     * Проверяет, принадлежит ли карта пользователю
     * @param cardId идентификатор карты
     * @param userId идентификатор пользователя
     * @return карта в формате DTO
     * @throws RuntimeException если карта не найдена или не принадлежит пользователю
     */
    public CardDto getUserCardById(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));
        return convertToDto(card);
    }

    /**
     * Обновление данных карты (только для ADMIN)
     * @param cardId идентификатор карты
     * @param request данные для обновления
     * @return обновленная карта в формате DTO
     * @throws RuntimeException если карта не найдена или данные невалидны
     */
    @Transactional
    public CardDto updateCard(Long cardId, UpdateCardRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + cardId));

        if (request.getBalance() != null) {
            validateBalance(request.getBalance());
            card.setBalance(request.getBalance());
        }

        if (request.getStatus() != null) {
            CardStatus status = validateAndParseStatus(request.getStatus());
            card.setStatus(status);
        }

        Card updatedCard = cardRepository.save(card);
        return convertToDto(updatedCard);
    }

    /**
     * Блокировка карты (только для ADMIN)
     * @param cardId идентификатор карты
     * @return заблокированная карта в формате DTO
     * @throws RuntimeException если карта не найдена
     */
    @Transactional
    public CardDto blockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + cardId));

        card.setStatus(CardStatus.BLOCKED);
        Card blockedCard = cardRepository.save(card);
        return convertToDto(blockedCard);
    }

    /**
     * Активация карты (только для ADMIN)
     * @param cardId идентификатор карты
     * @return активированная карта в формате DTO
     * @throws RuntimeException если карта не найдена или срок действия истек
     */
    @Transactional
    public CardDto activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + cardId));

        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot activate expired card. Expiry date: " + card.getExpiryDate());
        }

        card.setStatus(CardStatus.ACTIVE);
        Card activatedCard = cardRepository.save(card);
        return convertToDto(activatedCard);
    }

    /**
     * Удаление карты (только для ADMIN)
     * @param cardId идентификатор карты
     * @throws RuntimeException если карта не найдена
     */
    @Transactional
    public void deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + cardId));
        cardRepository.delete(card);
    }

    /**
     * Получение активных карт пользователя (для USER)
     * @param userId идентификатор пользователя
     * @return список активных карт пользователя
     */
    public List<CardDto> getUserActiveCards(Long userId) {
        return cardRepository.findByUserIdAndStatus(userId, CardStatus.ACTIVE).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение карт по статусу (только для ADMIN)
     * @param status строковое представление статуса
     * @return список карт с указанным статусом
     * @throws RuntimeException если статус невалиден
     */
    public List<CardDto> getCardsByStatus(String status) {
        CardStatus cardStatus = validateAndParseStatus(status);
        return cardRepository.findByStatus(cardStatus).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Автоматическое обновление статуса просроченных карт
     * Выполняется ежедневно в полночь
     */
    /*@Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void updateExpiredCards() {
        List<Card> expiredCards = cardRepository.findExpiredActiveCards();

        for (Card card : expiredCards) {
            card.setStatus(CardStatus.EXPIRED);
        }

        cardRepository.saveAll(expiredCards);
        System.out.println("Updated " + expiredCards.size() + " expired cards to EXPIRED status");
    }*/

    /**
     * Преобразование сущности Card в DTO с использованием Builder
     * @param card сущность карты
     * @return DTO объект карты
     */
    private CardDto convertToDto(Card card) {
        return CardDto.builder()
                .id(card.getId())
                .number(card.getNumber())
                .maskedNumber(card.getMaskedNumber())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus().name())
                .balance(card.getBalance())
                .userId(card.getUser().getId())
                .username(card.getUser().getUsername())
                .build();
    }

    /**
     * Валидация баланса карты
     * @param balance проверяемый баланс
     * @throws RuntimeException если баланс отрицательный
     */
    private void validateBalance(BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Balance cannot be negative: " + balance);
        }
    }

    /**
     * Валидация и преобразование строкового статуса в enum
     * @param status строковое представление статуса
     * @return enum значение статуса
     * @throws RuntimeException если статус невалиден
     */
    private CardStatus validateAndParseStatus(String status) {
        try {
            return CardStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid card status: " + status +
                    ". Valid values: ACTIVE, BLOCKED, EXPIRED");
        }
    }
}