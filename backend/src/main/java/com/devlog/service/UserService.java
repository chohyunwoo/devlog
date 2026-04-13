package com.devlog.service;

import com.devlog.common.exception.AuthenticationFailedException;
import com.devlog.common.exception.DuplicateEmailException;
import com.devlog.common.exception.DuplicateNicknameException;
import com.devlog.common.exception.DuplicateUserException;
import com.devlog.controller.dto.UserLoginRequest;
import com.devlog.controller.dto.UserSignupRequest;
import com.devlog.domain.User;
import com.devlog.repository.UserRepository;
import com.devlog.security.JwtProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    // 로그인 타이밍 공격 방어: 사용자 미존재 시에도 동일한 BCrypt 비용을 소비해 응답 시간을 평준화.
    // 실제 비밀번호와 매칭될 가능성은 사실상 0이며, 매칭되더라도 예외를 던지므로 인증에 영향 없음.
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public User signup(UserSignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new DuplicateNicknameException();
        }
        try {
            User user = User.create(
                    request.email(),
                    request.password(),
                    request.nickname(),
                    passwordEncoder);
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUserException();
        }
    }

    public LoginTokens login(UserLoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
            throw new AuthenticationFailedException();
        }
        if (!user.matchesPassword(request.password(), passwordEncoder)) {
            throw new AuthenticationFailedException();
        }
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail());
        return new LoginTokens(
                accessToken,
                refreshToken,
                jwtProvider.getAccessTokenExpirationSeconds());
    }
}
