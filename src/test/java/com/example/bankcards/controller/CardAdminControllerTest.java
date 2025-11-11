package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.UpdateCardRequest;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CardAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardService cardService;

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

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_WithValidData_ShouldReturnCard() throws Exception {
        CreateCardRequest request = CreateCardRequest.builder()
                .number("4111111111111111")
                .expiryDate(LocalDate.now().plusYears(1))
                .initialBalance(new BigDecimal("1000.00"))
                .userId(1L)
                .build();

        CardDto cardDto = createTestCardDto();

        when(cardService.createCard(any(CreateCardRequest.class))).thenReturn(cardDto);

        mockMvc.perform(post("/api/admin/cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.number").value("4111111111111111"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCards_ShouldReturnCardsList() throws Exception {
        CardDto cardDto = createTestCardDto();
        when(cardService.getAllCards()).thenReturn(List.of(cardDto));

        mockMvc.perform(get("/api/admin/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].number").value("4111111111111111"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCard_WithValidId_ShouldReturnCard() throws Exception {
        CardDto cardDto = createTestCardDto();
        when(cardService.getCardById(1L)).thenReturn(cardDto);

        mockMvc.perform(get("/api/admin/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.number").value("4111111111111111"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCard_WithValidData_ShouldReturnUpdatedCard() throws Exception {
        UpdateCardRequest request = UpdateCardRequest.builder()
                .balance(new BigDecimal("1500.00"))
                .status("BLOCKED")
                .build();

        CardDto cardDto = createTestCardDto();
        cardDto.setStatus("BLOCKED");
        cardDto.setBalance(new BigDecimal("1500.00"));

        when(cardService.updateCard(eq(1L), any(UpdateCardRequest.class))).thenReturn(cardDto);

        mockMvc.perform(put("/api/admin/cards/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.balance").value(1500.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blockCard_ShouldReturnBlockedCard() throws Exception {
        CardDto cardDto = createTestCardDto();
        cardDto.setStatus("BLOCKED");

        when(cardService.blockCard(1L)).thenReturn(cardDto);

        mockMvc.perform(post("/api/admin/cards/1/block")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activateCard_ShouldReturnActivatedCard() throws Exception {
        CardDto cardDto = createTestCardDto();
        cardDto.setStatus("ACTIVE");

        when(cardService.activateCard(1L)).thenReturn(cardDto);

        mockMvc.perform(post("/api/admin/cards/1/activate")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCard_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/cards/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(cardService).deleteCard(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCardsByStatus_ShouldReturnFilteredCards() throws Exception {
        CardDto cardDto = createTestCardDto();
        when(cardService.getCardsByStatus("ACTIVE")).thenReturn(List.of(cardDto));

        mockMvc.perform(get("/api/admin/cards/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCard_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        CreateCardRequest request = CreateCardRequest.builder()
                .number("4111111111111111")
                .expiryDate(LocalDate.now().plusYears(1))
                .userId(1L)
                .build();

        mockMvc.perform(post("/api/admin/cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
