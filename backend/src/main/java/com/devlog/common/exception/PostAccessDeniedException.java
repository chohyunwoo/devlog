package com.devlog.common.exception;

public class PostAccessDeniedException extends RuntimeException {

    public PostAccessDeniedException() {
        super("Not the post author");
    }
}
