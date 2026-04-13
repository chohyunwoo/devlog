package com.devlog.controller.dto;

import com.devlog.domain.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record PostCreateRequest(
        @NotBlank
        @Size(max = Post.MAX_TITLE_LENGTH)
        String title,

        @NotBlank
        String content,

        Set<@NotBlank @Size(max = Post.MAX_TAG_LENGTH) String> tags
) {
}
