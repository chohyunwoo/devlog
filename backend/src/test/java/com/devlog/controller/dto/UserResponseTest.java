package com.devlog.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.devlog.domain.User;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * UserResponse record의 매핑 및 민감 필드 노출 방지 회귀 테스트.
 */
class UserResponseTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("from()은 User의 id/email/nickname/createdAt을 그대로 매핑한다")
    void should_mapFieldsFromUser() throws Exception {
        // given
        User user = User.create("tester@devlog.com", "password1234", "tester", passwordEncoder);
        LocalDateTime now = LocalDateTime.of(2026, 4, 13, 10, 30, 0);
        // 영속화 전이라 id와 createdAt은 null이다. reflection으로 세팅한다.
        setField(user, "id", 42L);
        setField(user, "createdAt", now);

        // when
        UserResponse response = UserResponse.from(user);

        // then
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.email()).isEqualTo("tester@devlog.com");
        assertThat(response.nickname()).isEqualTo("tester");
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("UserResponse record에는 password 필드가 존재하지 않는다")
    void should_notContainPasswordField() {
        // given
        // 민감 정보가 응답 DTO에 섞여 들어가면 유출되므로 회귀 방어.

        // when
        boolean hasPasswordField = Arrays.stream(UserResponse.class.getDeclaredFields())
                .anyMatch(f -> f.getName().equalsIgnoreCase("password"));

        // then
        assertThat(hasPasswordField)
                .as("UserResponse에 password 필드가 추가되면 비밀번호가 응답으로 유출될 위험이 있다")
                .isFalse();
    }
}
