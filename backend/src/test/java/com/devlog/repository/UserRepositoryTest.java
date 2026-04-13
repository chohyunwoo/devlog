package com.devlog.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devlog.config.JpaAuditingConfig;
import com.devlog.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * UserRepository 슬라이스 테스트.
 *
 * <p>JPA 쿼리 파생 메서드(findByEmail, existsByEmail, existsByNickname)가
 * 실제 H2(MySQL 모드) 위에서 의도대로 동작하는지, 그리고 email/nickname
 * 유니크 제약이 DB 레벨에서 걸려 있는지(회귀 방지)를 검증한다.
 *
 * <p>User 엔티티의 @CreatedDate(nullable=false) 때문에 auditing이 활성화되지
 * 않으면 persist 자체가 실패하므로 JpaAuditingConfig를 함께 import 한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User newUser(String email, String nickname) {
        return User.create(email, "password1234", nickname, passwordEncoder);
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("저장된 email로 조회하면 해당 User가 반환된다")
        void should_returnUser_when_emailExists() {
            // given
            User saved = entityManager.persistFlushFind(newUser("tester@devlog.com", "tester"));

            // when
            Optional<User> found = userRepository.findByEmail("tester@devlog.com");

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
            assertThat(found.get().getEmail()).isEqualTo("tester@devlog.com");
            assertThat(found.get().getNickname()).isEqualTo("tester");
        }

        @Test
        @DisplayName("저장되지 않은 email로 조회하면 Optional.empty()가 반환된다")
        void should_returnEmpty_when_emailNotExists() {
            // given
            entityManager.persistFlushFind(newUser("tester@devlog.com", "tester"));

            // when
            Optional<User> found = userRepository.findByEmail("ghost@devlog.com");

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("저장된 email이면 true를 반환한다")
        void should_returnTrue_when_emailExists() {
            // given
            entityManager.persistFlushFind(newUser("tester@devlog.com", "tester"));

            // when
            boolean exists = userRepository.existsByEmail("tester@devlog.com");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("저장되지 않은 email이면 false를 반환한다")
        void should_returnFalse_when_emailNotExists() {
            // given
            entityManager.persistFlushFind(newUser("tester@devlog.com", "tester"));

            // when
            boolean exists = userRepository.existsByEmail("ghost@devlog.com");

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByNickname")
    class ExistsByNickname {

        @Test
        @DisplayName("저장된 nickname이면 true를 반환한다")
        void should_returnTrue_when_nicknameExists() {
            // given
            entityManager.persistFlushFind(newUser("tester@devlog.com", "tester"));

            // when
            boolean exists = userRepository.existsByNickname("tester");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("저장되지 않은 nickname이면 false를 반환한다")
        void should_returnFalse_when_nicknameNotExists() {
            // given
            entityManager.persistFlushFind(newUser("tester@devlog.com", "tester"));

            // when
            boolean exists = userRepository.existsByNickname("ghost");

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("유니크 제약 회귀 방어")
    class UniqueConstraints {

        /*
         * 참고: TestEntityManager는 Hibernate의 네이티브 EntityManager를 얇게 감싸므로
         * persist/flush 시 ConstraintViolationException이 그대로 올라온다.
         * Spring Data JPA의 Repository 프록시를 통해 저장하면
         * PersistenceExceptionTranslator가 개입해 DataIntegrityViolationException으로
         * 변환되므로, 유니크 제약 회귀 방어 테스트는 userRepository.saveAndFlush를 사용한다.
         */

        @Test
        @DisplayName("같은 email로 두 번째 User를 저장하면 DataIntegrityViolationException이 발생한다")
        void should_throwException_when_duplicateEmailPersisted() {
            // given
            entityManager.persistFlushFind(newUser("dup@devlog.com", "first"));
            entityManager.clear();
            User duplicate = newUser("dup@devlog.com", "second");

            // when & then
            assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("같은 nickname으로 두 번째 User를 저장하면 DataIntegrityViolationException이 발생한다")
        void should_throwException_when_duplicateNicknamePersisted() {
            // given
            entityManager.persistFlushFind(newUser("first@devlog.com", "sameNick"));
            entityManager.clear();
            User duplicate = newUser("second@devlog.com", "sameNick");

            // when & then
            assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
