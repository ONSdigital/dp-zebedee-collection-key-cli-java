package com.github.onsdigital.rekey;

public class RekeyException extends Exception {

    public RekeyException(String message) {
        super(message);
    }

    public RekeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
