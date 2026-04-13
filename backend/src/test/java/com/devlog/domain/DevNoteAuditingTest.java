package com.devlog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.devlog.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DevNote의 JPA Auditing 동작에 대한 슬라이스 테스트.
 * AuditingEntityListener + @EnableJpaAuditing이 함께 동작해야 하므로,
 * @DataJpaTest에 JpaAuditingConfig를 추가로 import한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class DevNoteAuditingTest {

    @Autowired
    private TestEntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User persistAuthor() {
        User author = User.create("author@devlog.com", "password1234", "author", passwordEncoder);
        return entityManager.persistFlushFind(author);
    }

    @Test
    @DisplayName("DevNote를 영속화하면 createdAt/updatedAt이 현재 시각 근처로 자동 세팅된다")
    void should_populateCreatedAtAndUpdatedAt_when_devNotePersisted() {
        // given
        User author = persistAuthor();
        DevNote devNote = DevNote.create("제목", "본문", author);

        // when
        DevNote persisted = entityManager.persistFlushFind(devNote);

        // then
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getCreatedAt())
                .as("@CreatedDate가 동작하지 않으면 null이 된다")
                .isNotNull()
                .isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(persisted.getUpdatedAt())
                .as("@LastModifiedDate가 동작하지 않으면 null이 된다")
                .isNotNull()
                .isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("update() 후 flush하면 updatedAt이 이전 값 이후로 갱신된다")
    void should_updateUpdatedAt_when_devNoteModified() throws Exception {
        // given
        User author = persistAuthor();
        DevNote devNote = DevNote.create("old", "old", author);
        DevNote persisted = entityManager.persistFlushFind(devNote);
        LocalDateTime originalUpdatedAt = persisted.getUpdatedAt();
        Long devNoteId = persisted.getId();

        // 영속성 컨텍스트 클리어 후 다시 꺼내서 수정 — Auditing이 진짜 동작하는지 확인
        entityManager.clear();

        // updatedAt 시간 해상도가 밀리초 이상인 DB도 커버할 수 있도록 약간의 지연
        Thread.sleep(20);

        // when
        DevNote loaded = entityManager.find(DevNote.class, devNoteId);
        loaded.update("new", "new");
        entityManager.flush();
        entityManager.clear();

        // then
        DevNote reloaded = entityManager.find(DevNote.class, devNoteId);
        assertThat(reloaded.getUpdatedAt())
                .as("@LastModifiedDate는 update flush 시점에 갱신되어야 한다")
                .isAfter(originalUpdatedAt);
        assertThat(reloaded.getTitle()).isEqualTo("new");
        assertThat(reloaded.getContent()).isEqualTo("new");
    }

    @Test
    @DisplayName("update() 후에도 createdAt은 변경되지 않는다")
    void should_keepCreatedAt_when_devNoteModified() throws Exception {
        // given
        User author = persistAuthor();
        DevNote persisted = entityManager.persistFlushFind(
                DevNote.create("old", "old", author));
        LocalDateTime originalCreatedAt = persisted.getCreatedAt();
        Long devNoteId = persisted.getId();

        entityManager.clear();
        Thread.sleep(20);

        // when
        DevNote loaded = entityManager.find(DevNote.class, devNoteId);
        loaded.update("new", "new");
        entityManager.flush();
        entityManager.clear();

        // then
        DevNote reloaded = entityManager.find(DevNote.class, devNoteId);
        assertThat(reloaded.getCreatedAt())
                .as("createdAt은 updatable=false이므로 변경되어서는 안 된다")
                .isEqualTo(originalCreatedAt);
    }
}
