package com.devlog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * User 엔티티의 순수 도메인 로직에 대한 단위 테스트.
 * Spring context 없이 실제 BCryptPasswordEncoder 인스턴스를 사용한다.
 */
class UserTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("create()는 전달받은 email과 nickname을 그대로 세팅한다")
    void should_setEmailAndNickname_when_created() {
        // given
        String email = "tester@devlog.com";
        String rawPassword = "password1234";
        String nickname = "tester";

        // when
        User user = User.create(email, rawPassword, nickname, passwordEncoder);

        // then
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getNickname()).isEqualTo(nickname);
    }

    @Test
    @DisplayName("create()는 raw password를 평문 그대로 저장하지 않고 해시로 저장한다")
    void should_storeEncodedPassword_when_created() throws Exception {
        // given
        String rawPassword = "password1234";
        User user = User.create("tester@devlog.com", rawPassword, "tester", passwordEncoder);

        // when
        // password 필드는 @Getter(AccessLevel.NONE)이므로 reflection으로 직접 꺼낸다.
        Field passwordField = User.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        String stored = (String) passwordField.get(user);

        // then
        assertThat(stored).isNotNull();
        assertThat(stored).isNotEqualTo(rawPassword);          // 평문 저장 금지
        assertThat(stored).startsWith("$2");                    // BCrypt 포맷 prefix
        assertThat(passwordEncoder.matches(rawPassword, stored)).isTrue();
    }

    @Test
    @DisplayName("matchesPassword()는 올바른 raw password에 대해 true를 반환한다")
    void should_returnTrue_when_rawPasswordMatches() {
        // given
        String rawPassword = "password1234";
        User user = User.create("tester@devlog.com", rawPassword, "tester", passwordEncoder);

        // when
        boolean result = user.matchesPassword(rawPassword, passwordEncoder);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("matchesPassword()는 다른 raw password에 대해 false를 반환한다")
    void should_returnFalse_when_rawPasswordDoesNotMatch() {
        // given
        User user = User.create("tester@devlog.com", "password1234", "tester", passwordEncoder);

        // when
        boolean result = user.matchesPassword("wrongPassword!!", passwordEncoder);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("User 클래스에는 public getPassword() 메서드가 노출되지 않는다")
    void should_notExposePublicPasswordGetter() {
        // given
        // @Getter(AccessLevel.NONE)이 제대로 걸렸는지 회귀 방어.

        // when
        boolean hasPublicGetter = false;
        for (Method method : User.class.getMethods()) {
            if (method.getName().equals("getPassword") && method.getParameterCount() == 0) {
                hasPublicGetter = true;
                break;
            }
        }

        // then
        assertThat(hasPublicGetter)
                .as("User.getPassword()가 public으로 노출되면 비밀번호가 유출될 위험이 있다")
                .isFalse();
    }
}
