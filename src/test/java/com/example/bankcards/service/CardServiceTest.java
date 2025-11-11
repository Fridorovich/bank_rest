package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private Card activeCard;
    private Card blockedCard;
    private Card expiredCard;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        activeCard = Card.builder()
                .id(1L)
                .number("4111111111111111")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .user(testUser)
                .build();

        blockedCard = Card.builder()
                .id(2L)
                .number("4222222222222222")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.BLOCKED)
                .balance(new BigDecimal("500.00"))
                .user(testUser)
                .build();

        expiredCard = Card.builder()
                .id(3L)
                .number("4333333333333333")
                .expiryDate(LocalDate.now().minusDays(1))
                .status(CardStatus.EXPIRED)
                .balance(new BigDecimal("0.00"))
                .user(testUser)
                .build();
    }

    @Test
    void createCard_WithValidData_ShouldCreateCard() {
        CreateCardRequest request = CreateCardRequest.builder()
                .number("4555555555555555")
                .expiryDate(LocalDate.now().plusYears(2))
                .initialBalance(new BigDecimal("1500.00"))
                .userId(1L)
                .build();

        when(cardRepository.existsByNumber("4555555555555555")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cardRepository.save(any(Card.class))).thenReturn(activeCard);

        CardDto result = cardService.createCard(request);

        assertNotNull(result);
        assertEquals("4111111111111111", result.getNumber());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_WhenNumberExists_ShouldThrowException() {
        CreateCardRequest request = CreateCardRequest.builder()
                .number("4111111111111111")
                .expiryDate(LocalDate.now().plusYears(1))
                .userId(1L)
                .build();

        when(cardRepository.existsByNumber("4111111111111111")).thenReturn(true);

        assertThrows(CardException.class, () -> cardService.createCard(request));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void createCard_WhenExpiryDateInPast_ShouldThrowException() {
        CreateCardRequest request = CreateCardRequest.builder()
                .number("4555555555555555")
                .expiryDate(LocalDate.now().minusDays(1))
                .userId(1L)
                .build();

        when(cardRepository.existsByNumber("4555555555555555")).thenReturn(false);

        assertThrows(CardException.class, () -> cardService.createCard(request));
    }

    @Test
    void transferBetweenOwnCards_WithValidData_ShouldTransfer() {
        TransferRequest request = TransferRequest.builder()
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("100.00"))
                .description("Test transfer")
                .build();

        Card fromCard = Card.builder()
                .id(1L)
                .number("4111111111111111")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .user(testUser)
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .number("4222222222222222")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("300.00"))
                .user(testUser)
                .build();

        when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(toCard));
        when(cardRepository.save(any(Card.class))).thenReturn(fromCard, toCard);

        TransferResponse result = cardService.transferBetweenOwnCards(request, 1L);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void transferBetweenOwnCards_WhenInsufficientFunds_ShouldThrowException() {
        TransferRequest request = TransferRequest.builder()
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("1000.00"))
                .build();

        Card fromCard = Card.builder()
                .id(1L)
                .number("4111111111111111")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .user(testUser)
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .number("4222222222222222")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("300.00"))
                .user(testUser)
                .build();

        when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(toCard));

        assertThrows(CardException.class, () -> cardService.transferBetweenOwnCards(request, 1L));
    }

    @Test
    void transferBetweenOwnCards_WhenCardBlocked_ShouldThrowException() {
        TransferRequest request = TransferRequest.builder()
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("100.00"))
                .build();

        Card fromCard = Card.builder()
                .id(1L)
                .number("4111111111111111")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.BLOCKED)
                .balance(new BigDecimal("500.00"))
                .user(testUser)
                .build();

        when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(fromCard));

        assertThrows(CardException.class, () -> cardService.transferBetweenOwnCards(request, 1L));
    }

    @Test
    void blockCard_ShouldBlockCard() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.save(any(Card.class))).thenReturn(blockedCard);

        CardDto result = cardService.blockCard(1L);

        assertNotNull(result);
        assertEquals("BLOCKED", result.getStatus());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void activateCard_WithValidCard_ShouldActivate() {
        when(cardRepository.findById(2L)).thenReturn(Optional.of(blockedCard));
        when(cardRepository.save(any(Card.class))).thenReturn(activeCard);

        CardDto result = cardService.activateCard(2L);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void activateCard_WhenCardExpired_ShouldThrowException() {
        when(cardRepository.findById(3L)).thenReturn(Optional.of(expiredCard));

        assertThrows(CardException.class, () -> cardService.activateCard(3L));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void getUserCardsWithFilter_ShouldReturnFilteredCards() {
        CardFilterRequest filter = CardFilterRequest.builder()
                .page(0)
                .size(10)
                .status(CardStatus.ACTIVE)
                .build();

        Page<Card> cardPage = new PageImpl<>(List.of(activeCard));
        when(cardRepository.findByUserIdAndStatus(eq(1L), eq(CardStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(cardPage);

        PageResponse<CardDto> result = cardService.getUserCardsWithFilter(1L, filter);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("ACTIVE", result.getContent().get(0).getStatus());
    }

    @Test
    void getCardBalance_ShouldReturnBalance() {
        when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(activeCard));

        BigDecimal result = cardService.getCardBalance(1L, 1L);

        assertEquals(new BigDecimal("1000.00"), result);
    }
}
