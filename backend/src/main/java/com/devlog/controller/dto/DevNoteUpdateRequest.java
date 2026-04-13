package com.devlog.controller.dto;

import com.devlog.domain.DevNote;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevNoteUpdateRequest(
        @NotBlank
        @Size(max = DevNote.MAX_TITLE_LENGTH)
        String title,

        @NotBlank
        String content
) {
}
