package com.devlog.controller.dto;

import com.devlog.domain.User;

public record AuthorSummary(Long id, String nickname) {

    public static AuthorSummary from(User user) {
        return new AuthorSummary(user.getId(), user.getNickname());
    }
}
