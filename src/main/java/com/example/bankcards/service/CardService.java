package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardException;
import com.example.bankcards.exception.UserException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
     * Создает новую банковскую карту для указанного пользователя
     * Выполняет валидацию номера карты и срока действия
     *
     * @param request данные для создания карты
     * @return созданная карта в формате DTO
     * @throws CardException если номер карты уже существует или срок действия истек
     * @throws UserException если пользователь не найден
     */
    @Transactional
    public CardDto createCard(CreateCardRequest request) {
        if (cardRepository.existsByNumber(request.getNumber())) {
            throw new CardException("Card with this number already exists: " + request.getNumber());
        }

        if (request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new CardException("Expiry date cannot be in the past: " + request.getExpiryDate());
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserException("User not found with id: " + request.getUserId()));

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
     * Получает список всех карт в системе
     *
     * @return список всех карт в системе
     */
    public List<CardDto> getAllCards() {
        return cardRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Получает карту по идентификатору
     *
     * @param cardId идентификатор карты
     * @return карта в формате DTO
     * @throws CardException если карта не найдена
     */
    public CardDto getCardById(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardException("Card not found with id: " + cardId));
        return convertToDto(card);
    }

    /**
     * Получает все карты указанного пользователя
     *
     * @param userId идентификатор пользователя
     * @return список карт пользователя
     */
    public List<CardDto> getUserCards(Long userId) {
        return cardRepository.findByUserId(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Получает конкретную карту пользователя с проверкой владения
     *
     * @param cardId идентификатор карты
     * @param userId идентификатор пользователя
     * @return карта в формате DTO
     * @throws CardException если карта не найдена или не принадлежит пользователю
     */
    public CardDto getUserCardById(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new CardException("Card not found or access denied"));
        return convertToDto(card);
    }

    /**
     * Обновляет данные карты
     * Позволяет изменить баланс и статус карты
     *
     * @param cardId идентификатор карты
     * @param request данные для обновления
     * @return обновленная карта в формате DTO
     * @throws CardException если карта не найдена или данные невалидны
     */
    @Transactional
    public CardDto updateCard(Long cardId, UpdateCardRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardException("Card not found with id: " + cardId));

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
     * Блокирует карту
     *
     * @param cardId идентификатор карты
     * @return заблокированная карта в формате DTO
     * @throws CardException если карта не найдена
     */
    @Transactional
    public CardDto blockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardException("Card not found with id: " + cardId));

        card.setStatus(CardStatus.BLOCKED);
        Card blockedCard = cardRepository.save(card);
        return convertToDto(blockedCard);
    }

    /**
     * Активирует карту
     * Проверяет срок действия карты перед активацией
     *
     * @param cardId идентификатор карты
     * @return активированная карта в формате DTO
     * @throws CardException если карта не найдена или срок действия истек
     */
    @Transactional
    public CardDto activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardException("Card not found with id: " + cardId));

        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new CardException("Cannot activate expired card. Expiry date: " + card.getExpiryDate());
        }

        card.setStatus(CardStatus.ACTIVE);
        Card activatedCard = cardRepository.save(card);
        return convertToDto(activatedCard);
    }

    /**
     * Удаляет карту из системы
     *
     * @param cardId идентификатор карты
     * @throws CardException если карта не найдена
     */
    @Transactional
    public void deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardException("Card not found with id: " + cardId));
        cardRepository.delete(card);
    }

    /**
     * Получает активные карты пользователя
     *
     * @param userId идентификатор пользователя
     * @return список активных карт пользователя
     */
    public List<CardDto> getUserActiveCards(Long userId) {
        return cardRepository.findByUserIdAndStatus(userId, CardStatus.ACTIVE).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Получает карты по статусу
     *
     * @param status строковое представление статуса
     * @return список карт с указанным статусом
     * @throws CardException если статус невалиден
     */
    public List<CardDto> getCardsByStatus(String status) {
        CardStatus cardStatus = validateAndParseStatus(status);
        return cardRepository.findByStatus(cardStatus).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Выполняет перевод средств между картами одного пользователя
     * Проверяет владение картами, их статус и достаточность средств
     *
     * @param request запрос на перевод
     * @param userId ID пользователя для проверки владения картами
     * @return информация о выполненной транзакции
     * @throws CardException если перевод невозможен
     */
    @Transactional
    public TransferResponse transferBetweenOwnCards(TransferRequest request, Long userId) {
        Card fromCard = cardRepository.findByIdAndUserId(request.getFromCardId(), userId)
                .orElseThrow(() -> new CardException("Source card not found or access denied"));

        Card toCard = cardRepository.findByIdAndUserId(request.getToCardId(), userId)
                .orElseThrow(() -> new CardException("Destination card not found or access denied"));

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
     * Обрабатывает запрос пользователя на блокировку карты
     *
     * @param cardId ID карты
     * @param userId ID пользователя
     * @param reason причина блокировки
     * @throws CardException если карта не найдена или не принадлежит пользователю
     */
    @Transactional
    public void requestCardBlock(Long cardId, Long userId, String reason) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new CardException("Card not found or access denied"));
    }

    /**
     * Получает отфильтрованный список карт пользователя с пагинацией
     * Поддерживает фильтрацию по статусу и поиск по последним цифрам номера
     *
     * @param userId ID пользователя
     * @param filter параметры фильтрации и пагинации
     * @return постраничный результат
     */
    public PageResponse<CardDto> getUserCardsWithFilter(Long userId, CardFilterRequest filter) {
        int page = Optional.ofNullable(filter.getPage()).filter(p -> p >= 0).orElse(0);
        int size = Optional.ofNullable(filter.getSize()).filter(s -> s > 0).orElse(10);

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
     * Получает баланс карты пользователя
     *
     * @param cardId ID карты
     * @param userId ID пользователя
     * @return баланс карты
     * @throws CardException если карта не найдена или не принадлежит пользователю
     */
    public BigDecimal getCardBalance(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new CardException("Card not found or access denied"));
        return card.getBalance();
    }

    /**
     * Получает историю переводов пользователя
     * В текущей реализации возвращает заглушку
     *
     * @param userId ID пользователя
     * @param page номер страницы
     * @param size размер страницы
     * @return постраничный список операций
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
     * Валидирует и форматирует поисковый запрос
     * Оставляет только цифры и обрезает до 4 последних символов
     */
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
     * Валидирует возможность перевода между картами
     * Проверяет статус карт, срок действия, достаточность средств и другие условия
     */
    private void validateTransfer(Card fromCard, Card toCard, BigDecimal amount) {
        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardException("Source card is not active. Current status: " + fromCard.getStatus());
        }

        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardException("Destination card is not active. Current status: " + toCard.getStatus());
        }

        if (fromCard.getExpiryDate().isBefore(LocalDate.now())) {
            throw new CardException("Source card is expired");
        }

        if (toCard.getExpiryDate().isBefore(LocalDate.now())) {
            throw new CardException("Destination card is expired");
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new CardException("Insufficient funds. Available: " + fromCard.getBalance());
        }

        if (fromCard.getId().equals(toCard.getId())) {
            throw new CardException("Cannot transfer to the same card");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CardException("Transfer amount must be positive");
        }

        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            throw new CardException("Transfer amount cannot exceed 1,000,000");
        }
    }

    /**
     * Преобразует страницу карт в PageResponse
     */
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
     * Преобразует сущность Card в DTO
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
     * Валидирует значение баланса карты
     *
     * @param balance проверяемый баланс
     * @throws CardException если баланс отрицательный
     */
    private void validateBalance(BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new CardException("Balance cannot be negative: " + balance);
        }
    }

    /**
     * Валидирует и преобразует строковый статус в enum
     *
     * @param status строковое представление статуса
     * @return enum значение статуса
     * @throws CardException если статус невалиден
     */
    private CardStatus validateAndParseStatus(String status) {
        try {
            return CardStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new CardException("Invalid card status: " + status +
                    ". Valid values: ACTIVE, BLOCKED, EXPIRED");
        }
    }
}