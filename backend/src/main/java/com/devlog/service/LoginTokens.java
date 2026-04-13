package com.devlog.service;

public record LoginTokens(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds
) {
}
