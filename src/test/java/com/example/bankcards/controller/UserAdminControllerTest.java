package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.service.UserService;
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

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private UserDto createTestUserDto() {
        return UserDto.builder()
                .id(1L)
                .username("testuser")
                .roles(Set.of("USER"))
                .cardsCount(3)
                .build();
    }

    private PageResponse<UserDto> createTestPageResponse() {
        return PageResponse.<UserDto>builder()
                .content(List.of(createTestUserDto()))
                .currentPage(0)
                .totalPages(1)
                .totalElements(1)
                .pageSize(10)
                .first(true)
                .last(true)
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsersFiltered_WithValidFilter_ShouldReturnUsers() throws Exception {
        UserFilterRequest filter = UserFilterRequest.builder()
                .page(0)
                .size(10)
                .build();

        PageResponse<UserDto> pageResponse = createTestPageResponse();
        when(userService.getAllUsersWithFilter(any(UserFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(post("/api/admin/users/filter")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("testuser"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUser_WithValidId_ShouldReturnUser() throws Exception {
        UserDto userDto = createTestUserDto();
        when(userService.getUserById(1L)).thenReturn(userDto);

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.cardsCount").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_WithValidData_ShouldReturnUser() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .password("password123")
                .roles(Set.of("USER"))
                .build();

        UserDto userDto = createTestUserDto();
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(userDto);

        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_WithValidData_ShouldReturnUpdatedUser() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .password("newpassword")
                .roles(Set.of("USER", "ADMIN"))
                .build();

        UserDto userDto = createTestUserDto();
        userDto.setRoles(Set.of("USER", "ADMIN"));

        when(userService.updateUser(eq(1L), any(UpdateUserRequest.class))).thenReturn(userDto);

        mockMvc.perform(put("/api/admin/users/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserStats_ShouldReturnStats() throws Exception {
        UserStatsDto statsDto = UserStatsDto.builder()
                .userId(1L)
                .username("testuser")
                .totalCards(5L)
                .activeCards(3L)
                .blockedCards(1L)
                .build();

        when(userService.getUserStats(1L)).thenReturn(statsDto);

        mockMvc.perform(get("/api/admin/users/1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCards").value(5))
                .andExpect(jsonPath("$.activeCards").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_ShouldReturnUsers() throws Exception {
        PageResponse<UserDto> pageResponse = createTestPageResponse();
        when(userService.getAllUsersWithFilter(any(UserFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("testuser"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createUser_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}