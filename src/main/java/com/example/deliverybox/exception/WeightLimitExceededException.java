package com.example.deliverybox.exception;

public class WeightLimitExceededException extends RuntimeException {

    public WeightLimitExceededException(String message) {
        super(message);
    }
}
