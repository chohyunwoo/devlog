package com.devlog.common.exception;

public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException() {
        super("User already registered");
    }

    public DuplicateUserException(String message) {
        super(message);
    }
}
