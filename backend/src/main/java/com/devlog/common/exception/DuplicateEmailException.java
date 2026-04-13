package com.devlog.common.exception;

public class DuplicateEmailException extends DuplicateUserException {

    public DuplicateEmailException() {
        super("Email already registered");
    }
}
