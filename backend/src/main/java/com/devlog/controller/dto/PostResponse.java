package com.devlog.controller.dto;

import com.devlog.domain.Post;
import java.time.LocalDateTime;
import java.util.Set;

public record PostResponse(
        Long id,
        String title,
        String content,
        AuthorSummary author,
        Set<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                AuthorSummary.from(post.getAuthor()),
                post.getTags(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
