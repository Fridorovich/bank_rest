// AuthControllerTest.java
package com.example.bankcards.controller;

import com.example.bankcards.dto.JwtDto;
import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void login_WithValidCredentials_ShouldReturnJwtTokens() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        JwtDto jwtDto = new JwtDto();
        jwtDto.setToken("accessToken");
        jwtDto.setRefreshToken("refreshToken");

        when(authService.login(any(LoginRequest.class))).thenReturn(jwtDto);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("accessToken"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken"));
    }

    @Test
    void register_WithValidData_ShouldReturnSuccess() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password123");

        when(authService.register(any(RegisterRequest.class))).thenReturn("User registered successfully");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void refreshToken_WithValidHeader_ShouldReturnNewTokens() throws Exception {
        JwtDto jwtDto = new JwtDto();
        jwtDto.setToken("newAccessToken");
        jwtDto.setRefreshToken("refreshToken");

        when(authService.refreshToken("validRefreshToken")).thenReturn(jwtDto);

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer validRefreshToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("newAccessToken"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken"));
    }

    @Test
    void refreshToken_WithInvalidHeader_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "InvalidHeader"))
                .andExpect(status().isBadRequest());
    }
}