package com.devlog.controller.dto;

import com.devlog.domain.DevNote;
import java.time.LocalDateTime;

public record DevNoteSummaryResponse(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DevNoteSummaryResponse from(DevNote devNote) {
        return new DevNoteSummaryResponse(
                devNote.getId(),
                devNote.getTitle(),
                devNote.getCreatedAt(),
                devNote.getUpdatedAt()
        );
    }
}
