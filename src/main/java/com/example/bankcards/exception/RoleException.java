package com.example.bankcards.exception;

/**
 * Исключение при работе с ролями
 */
public class RoleException extends BusinessException {
    public RoleException(String message) {
        super(message);
    }

    public RoleException(String message, Throwable cause) {
        super(message, cause);
    }
}