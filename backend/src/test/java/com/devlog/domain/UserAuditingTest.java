package com.devlog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.devlog.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * JPA Auditing (@CreatedDate) 슬라이스 테스트.
 * AuditingEntityListener + @EnableJpaAuditing이 함께 동작해야 하므로,
 * @DataJpaTest에 JpaAuditingConfig를 추가로 import하여 검증한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class UserAuditingTest {

    @Autowired
    private TestEntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("User를 영속화하면 createdAt이 자동으로 현재 시각 근처로 채워진다")
    void should_populateCreatedAt_when_userPersisted() {
        // given
        LocalDateTime before = LocalDateTime.now().minusSeconds(5);
        User user = User.create("tester@devlog.com", "password1234", "tester", passwordEncoder);

        // when
        User persisted = entityManager.persistFlushFind(user);
        LocalDateTime after = LocalDateTime.now().plusSeconds(5);

        // then
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getCreatedAt())
                .as("@CreatedDate가 동작하지 않으면 createdAt이 null이 된다")
                .isNotNull();
        assertThat(persisted.getCreatedAt())
                .as("createdAt은 현재 시각 근처여야 한다")
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }
}
