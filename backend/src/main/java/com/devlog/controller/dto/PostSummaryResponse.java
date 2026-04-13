package com.devlog.controller.dto;

import com.devlog.domain.Post;
import java.time.LocalDateTime;
import java.util.Set;

public record PostSummaryResponse(
        Long id,
        String title,
        AuthorSummary author,
        Set<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                AuthorSummary.from(post.getAuthor()),
                post.getTags(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
