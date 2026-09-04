package com.example.deliverybox.exception;

public class InvalidBoxStateException extends RuntimeException {

    public InvalidBoxStateException(String message) {
        super(message);
    }
}
