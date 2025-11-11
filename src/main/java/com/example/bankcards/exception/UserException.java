package com.example.bankcards.exception;

/**
 * Исключение при работе с пользователями
 */
public class UserException extends BusinessException {
    public UserException(String message) {
        super(message);
    }

    public UserException(String message, Throwable cause) {
        super(message, cause);
    }
}