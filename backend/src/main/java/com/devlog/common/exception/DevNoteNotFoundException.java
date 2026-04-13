package com.devlog.common.exception;

public class DevNoteNotFoundException extends RuntimeException {

    public DevNoteNotFoundException() {
        super("DevNote not found");
    }
}
