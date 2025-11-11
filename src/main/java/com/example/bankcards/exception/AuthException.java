package com.example.bankcards.exception;

/**
 * Исключение для операций аутентификации и авторизации
 */
public class AuthException extends BusinessException {
    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}