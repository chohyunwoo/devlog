package com.devlog.controller;

import com.devlog.controller.dto.LoginResponse;
import com.devlog.controller.dto.UserLoginRequest;
import com.devlog.controller.dto.UserResponse;
import com.devlog.controller.dto.UserSignupRequest;
import com.devlog.domain.User;
import com.devlog.service.LoginTokens;
import com.devlog.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody UserSignupRequest request) {
        User user = userService.signup(request);
        return UserResponse.from(user);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody UserLoginRequest request) {
        LoginTokens tokens = userService.login(request);
        return new LoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                TOKEN_TYPE_BEARER,
                tokens.accessTokenExpiresInSeconds());
    }
}
