package com.example.deliverybox.exception;

public class DuplicateBoxException extends RuntimeException {

    public DuplicateBoxException(String txref) {
        super("A box with txref '" + txref + "' already exists");
    }
}
