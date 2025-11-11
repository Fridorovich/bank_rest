package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления пользователями (только для ADMIN)
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Validated
public class UserAdminController {
    private final UserService userService;

    @PostMapping("/filter")
    public ResponseEntity<PageResponse<UserDto>> getUsersFiltered(
            @RequestBody UserFilterRequest filter) {
        PageResponse<UserDto> users = userService.getAllUsersWithFilter(filter);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long userId) {
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto user = userService.createUser(request);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        UserDto user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserStatsDto> getUserStats(@PathVariable Long userId) {
        UserStatsDto stats = userService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UserFilterRequest filter = UserFilterRequest.builder()
                .page(page)
                .size(size)
                .build();
        return getUsersFiltered(filter);
    }
}