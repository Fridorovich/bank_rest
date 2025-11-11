package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardException;
import com.example.bankcards.exception.RoleException;
import com.example.bankcards.exception.UserException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setRoles(Set.of(userRole));
    }

    @Test
    void getAllUsersWithFilter_ShouldReturnPageResponse() {
        UserFilterRequest filter = UserFilterRequest.builder()
                .page(0)
                .size(10)
                .build();

        Page<User> userPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);
        when(userRepository.countCardsByUserId(anyLong())).thenReturn(5L);

        PageResponse<UserDto> result = userService.getAllUsersWithFilter(filter);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("testuser", result.getContent().get(0).getUsername());
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUserDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.countCardsByUserId(1L)).thenReturn(3L);

        UserDto result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(3, result.getCardsCount());
        assertTrue(result.getRoles().contains("USER"));
    }

    @Test
    void getUserById_WhenUserNotExists_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserException.class, () -> userService.getUserById(999L));
    }

    @Test
    void createUser_WithValidData_ShouldCreateUser() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .password("password123")
                .roles(Set.of("USER"))
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRepository.countCardsByUserId(anyLong())).thenReturn(0L);

        UserDto result = userService.createUser(request);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void createUser_WhenUsernameExists_ShouldThrowException() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("existinguser")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(UserException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_WhenRoleNotExists_ShouldThrowException() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .password("password123")
                .roles(Set.of("INVALID_ROLE"))
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByName("INVALID_ROLE")).thenReturn(Optional.empty());

        assertThrows(RoleException.class, () -> userService.createUser(request));
    }

    @Test
    void updateUser_WithNewPassword_ShouldUpdateUser() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .password("newpassword")
                .roles(Set.of("USER", "ADMIN"))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("newpassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRepository.countCardsByUserId(1L)).thenReturn(2L);

        UserDto result = userService.updateUser(1L, request);

        assertNotNull(result);
        verify(passwordEncoder).encode("newpassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser_WhenUserHasNoCards_ShouldDeleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.countCardsByUserId(1L)).thenReturn(0L);

        userService.deleteUser(1L);

        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_WhenUserHasCards_ShouldThrowException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.countCardsByUserId(1L)).thenReturn(5L);

        assertThrows(UserException.class, () -> userService.deleteUser(1L));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void getUserStats_ShouldReturnUserStats() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.countCardsByUserId(1L)).thenReturn(5L);
        when(cardRepository.countByUserIdAndStatus(eq(1L), any())).thenReturn(3L);

        UserStatsDto result = userService.getUserStats(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(5L, result.getTotalCards());
        assertEquals(3L, result.getActiveCards());
        assertNull(result.getBlockedCards());
        verify(userRepository).findById(1L);
        verify(userRepository).countCardsByUserId(1L);
        verify(cardRepository).countByUserIdAndStatus(eq(1L), any());
    }
}
