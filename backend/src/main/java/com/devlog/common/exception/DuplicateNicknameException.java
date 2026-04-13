package com.devlog.common.exception;

public class DuplicateNicknameException extends DuplicateUserException {

    public DuplicateNicknameException() {
        super("Nickname already registered");
    }
}
