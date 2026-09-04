package com.example.deliverybox.exception;

public class LowBatteryException extends RuntimeException {

    public LowBatteryException(String message) {
        super(message);
    }
}
