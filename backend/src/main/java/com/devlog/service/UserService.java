package com.devlog.service;

import com.devlog.common.exception.DuplicateEmailException;
import com.devlog.common.exception.DuplicateNicknameException;
import com.devlog.common.exception.DuplicateUserException;
import com.devlog.controller.dto.UserSignupRequest;
import com.devlog.domain.User;
import com.devlog.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}
