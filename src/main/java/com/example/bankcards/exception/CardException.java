package com.example.bankcards.exception;

/**
 * Исключение при работе с картами
 */
public class CardException extends BusinessException {
    public CardException(String message) {
        super(message);
    }

    public CardException(String message, Throwable cause) {
        super(message, cause);
    }
}