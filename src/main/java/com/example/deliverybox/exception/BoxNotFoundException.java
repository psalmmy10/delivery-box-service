package com.example.deliverybox.exception;

public class BoxNotFoundException extends RuntimeException {

    public BoxNotFoundException(String txref) {
        super("Box not found with txref: " + txref);
    }
}
