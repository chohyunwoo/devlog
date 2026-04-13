package com.devlog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.devlog.common.exception.DuplicateEmailException;
import com.devlog.common.exception.DuplicateNicknameException;
import com.devlog.common.exception.DuplicateUserException;
import com.devlog.controller.dto.UserSignupRequest;
import com.devlog.domain.User;
import com.devlog.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private static UserSignupRequest request(String email, String password, String nickname) {
        return new UserSignupRequest(email, password, nickname);
    }

    private static UserSignupRequest validRequest() {
        return request("user@devlog.com", "password123", "devlog-user");
    }

    // ---------------------------------------------------------------------
    // 1. 정상 가입
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("정상 가입 흐름")
    class HappyPath {

        @Test
        @DisplayName("중복이 없으면 인코딩된 비밀번호로 User를 생성하고 저장한다")
        void should_saveUserWithEncodedPassword_when_noDuplicates() {
            // given
            UserSignupRequest req = validRequest();
            given(userRepository.existsByEmail(req.email())).willReturn(false);
            given(userRepository.existsByNickname(req.nickname())).willReturn(false);
            given(passwordEncoder.encode(req.password())).willReturn("encoded-" + req.password());
            // save 는 인자로 받은 User 를 그대로 반환 (영속화 흉내)
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            User result = userService.signup(req);

            // then: 반환 User 상태
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(req.email());
            assertThat(result.getNickname()).isEqualTo(req.nickname());

            // then: save 에 전달된 User 의 필드 값 검증 (ArgumentCaptor)
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getEmail()).isEqualTo(req.email());
            assertThat(saved.getNickname()).isEqualTo(req.nickname());
            // User.getPassword() 는 AccessLevel.NONE 이므로 matchesPassword 경로로 검증
            // passwordEncoder.matches 스텁: encoded 값이 정확히 일치하면 true
            given(passwordEncoder.matches(req.password(), "encoded-" + req.password())).willReturn(true);
            assertThat(saved.matchesPassword(req.password(), passwordEncoder)).isTrue();

            // then: 인코더는 정확히 한 번 호출
            verify(passwordEncoder).encode(req.password());
        }
    }

    // ---------------------------------------------------------------------
    // 2. 중복 검사
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("중복 검사 실패")
    class DuplicateChecks {

        @Test
        @DisplayName("이메일이 이미 존재하면 DuplicateEmailException 을 던지고 save 는 호출되지 않는다")
        void should_throwDuplicateEmail_when_emailAlreadyExists() {
            // given
            UserSignupRequest req = validRequest();
            given(userRepository.existsByEmail(req.email())).willReturn(true);

            // when / then
            assertThatThrownBy(() -> userService.signup(req))
                    .isInstanceOf(DuplicateEmailException.class);

            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("이메일 중복이면 existsByNickname 은 호출되지 않는다 (pre-check 순서)")
        void should_shortCircuit_when_emailDuplicateDetected() {
            // given
            UserSignupRequest req = validRequest();
            given(userRepository.existsByEmail(req.email())).willReturn(true);

            // when
            assertThatThrownBy(() -> userService.signup(req))
                    .isInstanceOf(DuplicateEmailException.class);

            // then: 이메일에서 이미 실패했으므로 닉네임 검사로 넘어가면 안 된다
            verify(userRepository).existsByEmail(req.email());
            verify(userRepository, never()).existsByNickname(anyString());
        }

        @Test
        @DisplayName("닉네임이 이미 존재하면 DuplicateNicknameException 을 던지고 save 는 호출되지 않는다")
        void should_throwDuplicateNickname_when_nicknameAlreadyExists() {
            // given
            UserSignupRequest req = validRequest();
            given(userRepository.existsByEmail(req.email())).willReturn(false);
            given(userRepository.existsByNickname(req.nickname())).willReturn(true);

            // when / then
            assertThatThrownBy(() -> userService.signup(req))
                    .isInstanceOf(DuplicateNicknameException.class);

            verify(userRepository).existsByEmail(req.email());
            verify(userRepository).existsByNickname(req.nickname());
            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // ---------------------------------------------------------------------
    // 3. TOCTOU race: exists 는 통과했지만 save 에서 DB 유니크 제약 위반
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("TOCTOU race fallback")
    class TocTouRace {

        @Test
        @DisplayName("save 가 DataIntegrityViolationException 을 던지면 DuplicateUserException 으로 변환한다")
        void should_translateDataIntegrityViolation_toDuplicateUserException() {
            // given: 두 exists 모두 통과했지만 다른 트랜잭션이 먼저 insert 한 상황
            UserSignupRequest req = validRequest();
            given(userRepository.existsByEmail(req.email())).willReturn(false);
            given(userRepository.existsByNickname(req.nickname())).willReturn(false);
            given(passwordEncoder.encode(req.password())).willReturn("encoded-" + req.password());
            given(userRepository.save(any(User.class)))
                    .willThrow(new DataIntegrityViolationException("unique constraint violated"));

            // when / then
            assertThatThrownBy(() -> userService.signup(req))
                    .isInstanceOf(DuplicateUserException.class);

            verify(userRepository).save(any(User.class));
        }
    }
}
