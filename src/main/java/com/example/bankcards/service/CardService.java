package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Сервис для управления банковскими картами
 * Реализует CRUD операции, переводы, фильтрацию с пагинацией
 */
@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    private final AtomicLong transactionIdGenerator = new AtomicLong(1);

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
     * Перевод средств между картами пользователя
     * @param request запрос на перевод
     * @param userId ID пользователя (для проверки владения картами)
     * @return информация о выполненной транзакции
     * @throws RuntimeException если перевод невозможен
     */
    @Transactional
    public TransferResponse transferBetweenOwnCards(TransferRequest request, Long userId) {
        Card fromCard = cardRepository.findByIdAndUserId(request.getFromCardId(), userId)
                .orElseThrow(() -> new RuntimeException("Source card not found or access denied"));

        Card toCard = cardRepository.findByIdAndUserId(request.getToCardId(), userId)
                .orElseThrow(() -> new RuntimeException("Destination card not found or access denied"));

        validateTransfer(fromCard, toCard, request.getAmount());

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        return TransferResponse.builder()
                .transactionId(transactionIdGenerator.getAndIncrement())
                .fromCardMasked(fromCard.getMaskedNumber())
                .toCardMasked(toCard.getMaskedNumber())
                .amount(request.getAmount())
                .description(request.getDescription())
                .timestamp(LocalDateTime.now())
                .status("COMPLETED")
                .build();
    }

    /**
     * Запрос на блокировку карты (иницируется пользователем)
     * @param cardId ID карты
     * @param userId ID пользователя
     * @param reason причина блокировки
     * @throws RuntimeException если карта не найдена или не принадлежит пользователю
     */
    @Transactional
    public void requestCardBlock(Long cardId, Long userId, String reason) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));

        System.out.println("Block request for card: " + card.getMaskedNumber() +
                ", User: " + userId + ", Reason: " + reason);

    }

    /**
     * Получение отфильтрованного списка карт пользователя с пагинацией
     * @param userId ID пользователя
     * @param filter параметры фильтрации
     * @return постраничный результат
     */
    public PageResponse<CardDto> getUserCardsWithFilter(Long userId, CardFilterRequest filter) {
        int page = filter.getPage() != null && filter.getPage() >= 0 ? filter.getPage() : 0;
        int size = filter.getSize() != null && filter.getSize() > 0 ? filter.getSize() : 10;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Card> cardPage;

        String validatedSearchTerm = validateAndFormatSearchTerm(filter.getSearchTerm());

        if (filter.getStatus() != null && validatedSearchTerm != null) {
            cardPage = cardRepository.findByUserIdAndStatusAndLastFourDigits(
                    userId, filter.getStatus(), validatedSearchTerm, pageable);
        } else if (filter.getStatus() != null) {
            cardPage = cardRepository.findByUserIdAndStatus(userId, filter.getStatus(), pageable);
        } else if (validatedSearchTerm != null) {
            cardPage = cardRepository.findByUserIdAndLastFourDigits(userId, validatedSearchTerm, pageable);
        } else {
            cardPage = cardRepository.findByUserId(userId, pageable);
        }

        return convertToPageResponse(cardPage);
    }


    /**
     * Получение баланса карты пользователя
     * @param cardId ID карты
     * @param userId ID пользователя
     * @return баланс карты
     * @throws RuntimeException если карта не найдена или не принадлежит пользователю
     */
    public BigDecimal getCardBalance(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));
        return card.getBalance();
    }

    /**
     * Получение истории переводов пользователя (имитация - в реальном приложении была бы отдельная таблица)
     * @param userId ID пользователя
     * @param page номер страницы
     * @param size размер страницы
     * @return постраничный список последних операций (заглушка)
     */
    public PageResponse<TransferResponse> getTransferHistory(Long userId, int page, int size) {
        List<TransferResponse> emptyList = List.of();

        return PageResponse.<TransferResponse>builder()
                .content(emptyList)
                .currentPage(page)
                .totalPages(0)
                .totalElements(0)
                .pageSize(size)
                .first(true)
                .last(true)
                .build();
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

    private String validateAndFormatSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return null;
        }

        String digitsOnly = searchTerm.replaceAll("[^0-9]", "");

        if (digitsOnly.length() > 4) {
            digitsOnly = digitsOnly.substring(digitsOnly.length() - 4);
        }

        return digitsOnly.isEmpty() ? null : digitsOnly;
    }

    /**
     * Валидация перевода между картами
     */
    private void validateTransfer(Card fromCard, Card toCard, BigDecimal amount) {
        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new RuntimeException("Source card is not active. Current status: " + fromCard.getStatus());
        }

        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new RuntimeException("Destination card is not active. Current status: " + toCard.getStatus());
        }

        if (fromCard.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Source card is expired");
        }

        if (toCard.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Destination card is expired");
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds. Available: " + fromCard.getBalance());
        }

        if (fromCard.getId().equals(toCard.getId())) {
            throw new RuntimeException("Cannot transfer to the same card");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be positive");
        }

        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            throw new RuntimeException("Transfer amount cannot exceed 1,000,000");
        }
    }

    private PageResponse<CardDto> convertToPageResponse(Page<Card> cardPage) {
        List<CardDto> cardDtos = cardPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return PageResponse.<CardDto>builder()
                .content(cardDtos)
                .currentPage(cardPage.getNumber())
                .totalPages(cardPage.getTotalPages())
                .totalElements(cardPage.getTotalElements())
                .pageSize(cardPage.getSize())
                .first(cardPage.isFirst())
                .last(cardPage.isLast())
                .build();
    }

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