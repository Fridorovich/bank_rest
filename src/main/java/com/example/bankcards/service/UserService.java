package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardException;
import com.example.bankcards.exception.RoleException;
import com.example.bankcards.exception.UserException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для управления пользователями системы
 * Предоставляет полный CRUD функционал для администраторов
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Получает список всех пользователей с поддержкой фильтрации и пагинации
     * Позволяет фильтровать пользователей по имени и роли с постраничным выводом
     *
     * @param filter объект с параметрами фильтрации и пагинации
     * @return постраничный результат с информацией о пользователях
     * @throws UserException если произошла ошибка при получении данных
     */
    public PageResponse<UserDto> getAllUsersWithFilter(UserFilterRequest filter) {
        try {
            int page = Optional.ofNullable(filter.getPage()).filter(p -> p >= 0).orElse(0);
            int size = Optional.ofNullable(filter.getSize()).filter(s -> s > 0).orElse(10);

            Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDirection())
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortBy = Optional.ofNullable(filter.getSortBy()).filter(s -> !s.trim().isEmpty()).orElse("id");

            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<User> userPage = applyFilters(filter, pageable);

            return convertToPageResponse(userPage);

        } catch (Exception e) {
            throw new UserException("Failed to retrieve users: " + e.getMessage(), e);
        }
    }

    /**
     * Получает детальную информацию о пользователе по его идентификатору
     * Включает информацию о ролях и количестве карт пользователя
     *
     * @param userId уникальный идентификатор пользователя
     * @return объект с полной информацией о пользователе
     * @throws UserException если пользователь с указанным ID не найден
     */
    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with ID: " + userId));
        return convertToDto(user);
    }

    /**
     * Создает нового пользователя в системе
     * Автоматически хеширует пароль и назначает указанные роли
     * Если роли не указаны, назначается роль USER по умолчанию
     *
     * @param request объект с данными для создания пользователя
     * @return созданный пользователь
     * @throws UserException если пользователь с таким именем уже существует
     * @throws RoleException если указанные роли не найдены в системе
     */
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserException("Username already exists: " + request.getUsername());
        }

        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            Set<Role> roles = getValidatedRoles(request.getRoles());
            user.setRoles(roles);

            User savedUser = userRepository.save(user);
            return convertToDto(savedUser);

        } catch (RoleException e) {
            throw e;
        } catch (Exception e) {
            throw new UserException("Failed to create user: " + e.getMessage(), e);
        }
    }

    /**
     * Обновляет информацию о существующем пользователе
     * Позволяет изменить пароль и набор ролей пользователя
     * Пароль обновляется только если предоставлен новый пароль
     *
     * @param userId уникальный идентификатор пользователя
     * @param request объект с данными для обновления
     * @return обновленный пользователь
     * @throws UserException если пользователь с указанным ID не найден
     * @throws RoleException если указанные роли не найдены в системе
     */
    @Transactional
    public UserDto updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with ID: " + userId));

        try {
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            if (request.getRoles() != null) {
                Set<Role> roles = getValidatedRoles(request.getRoles());
                user.setRoles(roles);
            }

            User updatedUser = userRepository.save(user);
            return convertToDto(updatedUser);

        } catch (RoleException e) {
            throw e;
        } catch (Exception e) {
            throw new UserException("Failed to update user: " + e.getMessage(), e);
        }
    }

    /**
     * Удаляет пользователя из системы
     * Перед удалением проверяет, что у пользователя нет активных карт
     *
     * @param userId уникальный идентификатор пользователя
     * @throws UserException если пользователь не найден или имеет активные карты
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with ID: " + userId));

        Long cardsCount = userRepository.countCardsByUserId(userId);
        if (cardsCount > 0) {
            throw new UserException("Cannot delete user with active cards. User has " + cardsCount + " card(s)");
        }

        try {
            userRepository.delete(user);
        } catch (Exception e) {
            throw new UserException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    /**
     * Получает статистику по пользователю
     * Включает общее количество карт и количество активных карт
     *
     * @param userId уникальный идентификатор пользователя
     * @return объект со статистикой пользователя
     * @throws UserException если пользователь не найден
     * @throws CardException если произошла ошибка при получении данных о картах
     */
    public UserStatsDto getUserStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with ID: " + userId));

        try {
            Long cardsCount = userRepository.countCardsByUserId(userId);
            Long activeCardsCount = cardRepository.countByUserIdAndStatus(userId, CardStatus.ACTIVE);

            return UserStatsDto.builder()
                    .userId(userId)
                    .username(user.getUsername())
                    .totalCards(cardsCount)
                    .activeCards(activeCardsCount)
                    .build();

        } catch (Exception e) {
            throw new CardException("Failed to get user statistics: " + e.getMessage(), e);
        }
    }

    /**
     * Применяет фильтры к запросу пользователей
     */
    private Page<User> applyFilters(UserFilterRequest filter, Pageable pageable) {
        if (filter.getUsername() != null && filter.getRole() != null) {
            return userRepository.findByUsernameContainingAndRoleName(
                    filter.getUsername(), filter.getRole(), pageable);
        } else if (filter.getUsername() != null) {
            return userRepository.findByUsernameContainingIgnoreCase(filter.getUsername(), pageable);
        } else if (filter.getRole() != null) {
            return userRepository.findByRoleName(filter.getRole(), pageable);
        } else {
            return userRepository.findAll(pageable);
        }
    }

    /**
     * Валидирует и получает объекты ролей по их названиям
     */
    private Set<Role> getValidatedRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RoleException("Default role USER not found"));
            return Set.of(userRole);
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RoleException("Role not found: " + roleName));
            roles.add(role);
        }
        return roles;
    }

    /**
     * Преобразует сущность User в DTO
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
     * Преобразует страницу пользователей в PageResponse
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