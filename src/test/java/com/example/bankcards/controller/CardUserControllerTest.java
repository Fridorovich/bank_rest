package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CardUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardService cardService;

    private void setupAuthentication() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private CardDto createTestCardDto() {
        return CardDto.builder()
                .id(1L)
                .number("4111111111111111")
                .maskedNumber("**** **** **** 1111")
                .expiryDate(LocalDate.now().plusYears(1))
                .status("ACTIVE")
                .balance(new BigDecimal("1000.00"))
                .userId(1L)
                .username("testuser")
                .build();
    }

    private PageResponse<CardDto> createTestPageResponse() {
        return PageResponse.<CardDto>builder()
                .content(List.of(createTestCardDto()))
                .currentPage(0)
                .totalPages(1)
                .totalElements(1)
                .pageSize(10)
                .first(true)
                .last(true)
                .build();
    }

    @Test
    void getUserCard_WithValidId_ShouldReturnCard() throws Exception {
        setupAuthentication();
        CardDto cardDto = createTestCardDto();
        when(cardService.getUserCardById(1L, 1L)).thenReturn(cardDto);

        mockMvc.perform(get("/api/user/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.number").value("4111111111111111"));
    }

    @Test
    void getUserCardsFiltered_WithValidFilter_ShouldReturnCards() throws Exception {
        setupAuthentication();
        CardFilterRequest filter = CardFilterRequest.builder()
                .status(CardStatus.ACTIVE)
                .page(0)
                .size(10)
                .build();

        PageResponse<CardDto> pageResponse = createTestPageResponse();
        when(cardService.getUserCardsWithFilter(eq(1L), any(CardFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(post("/api/user/cards/filter")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void transferBetweenCards_WithValidRequest_ShouldReturnTransfer() throws Exception {
        setupAuthentication();
        TransferRequest request = TransferRequest.builder()
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("100.00"))
                .description("Test transfer")
                .build();

        TransferResponse transferResponse = TransferResponse.builder()
                .transactionId(1L)
                .fromCardMasked("**** **** **** 1111")
                .toCardMasked("**** **** **** 2222")
                .amount(new BigDecimal("100.00"))
                .description("Test transfer")
                .timestamp(LocalDateTime.now())
                .status("COMPLETED")
                .build();

        when(cardService.transferBetweenOwnCards(any(TransferRequest.class), eq(1L))).thenReturn(transferResponse);

        mockMvc.perform(post("/api/user/cards/transfer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    void requestCardBlock_WithValidRequest_ShouldReturnSuccess() throws Exception {
        setupAuthentication();
        BlockCardRequest request = BlockCardRequest.builder()
                .reason("Card lost")
                .build();

        mockMvc.perform(post("/api/user/cards/1/block-request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Block request submitted successfully. Administrator will review it."));
    }

    @Test
    void getCardBalance_WithValidCard_ShouldReturnBalance() throws Exception {
        setupAuthentication();
        when(cardService.getCardBalance(1L, 1L)).thenReturn(new BigDecimal("1000.00"));

        mockMvc.perform(get("/api/user/cards/1/balance"))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.00"));
    }

    @Test
    void getTransferHistory_ShouldReturnHistory() throws Exception {
        setupAuthentication();
        PageResponse<TransferResponse> pageResponse = PageResponse.<TransferResponse>builder()
                .content(List.of())
                .currentPage(0)
                .totalPages(0)
                .totalElements(0)
                .pageSize(10)
                .first(true)
                .last(true)
                .build();

        when(cardService.getTransferHistory(1L, 0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/user/cards/transfer-history")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getActiveCards_ShouldReturnActiveCards() throws Exception {
        setupAuthentication();
        PageResponse<CardDto> pageResponse = createTestPageResponse();
        when(cardService.getUserCardsWithFilter(eq(1L), any(CardFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/user/cards/active")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void getUserCards_ShouldReturnAllCards() throws Exception {
        setupAuthentication();
        PageResponse<CardDto> pageResponse = createTestPageResponse();
        when(cardService.getUserCardsWithFilter(eq(1L), any(CardFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/user/cards")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }
}
