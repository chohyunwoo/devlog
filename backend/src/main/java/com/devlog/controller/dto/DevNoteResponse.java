package com.devlog.controller.dto;

import com.devlog.domain.DevNote;
import java.time.LocalDateTime;

public record DevNoteResponse(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DevNoteResponse from(DevNote devNote) {
        return new DevNoteResponse(
                devNote.getId(),
                devNote.getTitle(),
                devNote.getContent(),
                devNote.getCreatedAt(),
                devNote.getUpdatedAt()
        );
    }
}
