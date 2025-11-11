package com.example.bankcards.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import java.util.Set;

/**
 * DTO для обновления пользователя
 */
@Data
@Builder
public class UpdateUserRequest {

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private Set<String> roles;
    private Boolean active;
}
