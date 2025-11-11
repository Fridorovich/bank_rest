package com.example.bankcards.service;

import com.example.bankcards.dto.JwtDto;
import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AuthException;
import com.example.bankcards.exception.RoleException;
import com.example.bankcards.exception.UserException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Сервис для аутентификации и регистрации пользователей
 * Обрабатывает вход в систему, регистрацию и обновление токенов
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Выполняет аутентификацию пользователя в системе
     * Проверяет учетные данные и генерирует JWT токены при успешной аутентификации
     *
     * @param loginRequest объект с учетными данными пользователя
     * @return JWT токены доступа и обновления
     * @throws AuthException если аутентификация не удалась
     */
    public JwtDto login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            return jwtService.generateAuthToken(user);

        } catch (BadCredentialsException e) {
            throw new AuthException("Invalid username or password");
        } catch (Exception e) {
            throw new AuthException("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Регистрирует нового пользователя в системе
     * Создает учетную запись с ролью USER по умолчанию
     *
     * @param registerRequest объект с данными для регистрации
     * @return сообщение об успешной регистрации
     * @throws UserException если пользователь с таким именем уже существует
     * @throws AuthException если пароли не совпадают
     * @throws RoleException если роль USER не найдена в системе
     */
    @Transactional
    public String register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UserException("Username already exists: " + registerRequest.getUsername());
        }

        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new AuthException("Passwords do not match");
        }

        try {
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RoleException("Role USER not found"));
            roles.add(userRole);
            user.setRoles(roles);

            userRepository.save(user);

            return "User registered successfully";

        } catch (RoleException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Обновляет access token с использованием refresh token
     * Валидирует refresh token и генерирует новую пару токенов
     *
     * @param refreshToken токен для обновления
     * @return новая пара JWT токенов
     * @throws AuthException если refresh token невалиден или пользователь не найден
     */
    public JwtDto refreshToken(String refreshToken) {
        if (!jwtService.validateJwtToken(refreshToken)) {
            throw new AuthException("Invalid refresh token");
        }

        try {
            String username = jwtService.getUsernameFromToken(refreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserException("User not found"));

            return jwtService.refreshBaseToken(user, refreshToken);

        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("Token refresh failed: " + e.getMessage());
        }
    }
}