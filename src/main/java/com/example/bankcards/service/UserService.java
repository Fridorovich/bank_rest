package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для управления пользователями (только для ADMIN)
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Получение всех пользователей с фильтрацией и пагинацией
     */
    public PageResponse<UserDto> getAllUsersWithFilter(UserFilterRequest filter) {
        int page = filter.getPage() != null && filter.getPage() >= 0 ? filter.getPage() : 0;
        int size = filter.getSize() != null && filter.getSize() > 0 ? filter.getSize() : 10;

        Sort.Direction direction = filter.getSortDirection().equals("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, filter.getSortBy()));

        Page<User> userPage;

        if (filter.getUsername() != null && filter.getRole() != null) {
            userPage = userRepository.findByUsernameContainingAndRoleName(
                    filter.getUsername(), filter.getRole(), pageable);
        } else if (filter.getUsername() != null) {
            userPage = userRepository.findByUsernameContainingIgnoreCase(filter.getUsername(), pageable);
        } else if (filter.getRole() != null) {
            userPage = userRepository.findByRoleName(filter.getRole(), pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        return convertToPageResponse(userPage);
    }

    /**
     * Получение пользователя по ID
     */
    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return convertToDto(user);
    }

    /**
     * Создание нового пользователя
     */
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<Role> roles = getValidatedRoles(request.getRoles());
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    /**
     * Обновление пользователя
     */
    @Transactional
    public UserDto updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoles() != null) {
            Set<Role> roles = getValidatedRoles(request.getRoles());
            user.setRoles(roles);
        }

        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    /**
     * Удаление пользователя
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Long cardsCount = userRepository.countCardsByUserId(userId);
        if (cardsCount > 0) {
            throw new RuntimeException("Cannot delete user with active cards. User has " + cardsCount + " card(s)");
        }

        userRepository.delete(user);
    }

    /**
     * Получение статистики по пользователю
     */
    public UserStatsDto getUserStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Long cardsCount = userRepository.countCardsByUserId(userId);
        Long activeCardsCount = cardRepository.countByUserIdAndStatus(userId, CardStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));;

        return UserStatsDto.builder()
                .userId(userId)
                .username(user.getUsername())
                .totalCards(cardsCount)
                .activeCards(activeCardsCount)
                .build();
    }

    /**
     * Валидация и получение ролей
     */
    private Set<Role> getValidatedRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Role USER not found"));
            return Set.of(userRole);
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            roles.add(role);
        }
        return roles;
    }

    /**
     * Преобразование User в UserDto
     */
    private UserDto convertToDto(User user) {
        Long cardsCount = userRepository.countCardsByUserId(user.getId());

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .cardsCount(cardsCount.intValue())
                .build();
    }

    /**
     * Преобразование Page<User> в PageResponse<UserDto>
     */
    private PageResponse<UserDto> convertToPageResponse(Page<User> userPage) {
        List<UserDto> userDtos = userPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return PageResponse.<UserDto>builder()
                .content(userDtos)
                .currentPage(userPage.getNumber())
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .pageSize(userPage.getSize())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .build();
    }
}

