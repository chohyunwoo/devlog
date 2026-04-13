package com.devlog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.devlog.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Post의 JPA Auditing/@ElementCollection 동작에 대한 슬라이스 테스트.
 * AuditingEntityListener + @EnableJpaAuditing이 함께 동작해야 하므로,
 * @DataJpaTest에 JpaAuditingConfig를 추가로 import한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class PostAuditingTest {

    @Autowired
    private TestEntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User persistAuthor() {
        User author = User.create("author@devlog.com", "password1234", "author", passwordEncoder);
        return entityManager.persistFlushFind(author);
    }

    @Test
    @DisplayName("Post를 영속화하면 createdAt/updatedAt이 현재 시각 근처로 자동 세팅된다")
    void should_populateCreatedAtAndUpdatedAt_when_postPersisted() {
        // given
        User author = persistAuthor();
        Post post = Post.create("제목", "본문", author,
                new LinkedHashSet<>(List.of("java", "spring")));

        // when
        Post persisted = entityManager.persistFlushFind(post);

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
    void should_updateUpdatedAt_when_postModified() throws Exception {
        // given
        User author = persistAuthor();
        Post post = Post.create("old", "old", author,
                new LinkedHashSet<>(List.of("java")));
        Post persisted = entityManager.persistFlushFind(post);
        LocalDateTime originalUpdatedAt = persisted.getUpdatedAt();
        Long postId = persisted.getId();

        // 영속성 컨텍스트 클리어 후 다시 꺼내서 수정 — Auditing이 진짜 동작하는지 확인
        entityManager.clear();

        // updatedAt 시간 해상도가 밀리초 이상인 DB도 커버할 수 있도록 약간의 지연
        Thread.sleep(20);

        // when
        Post loaded = entityManager.find(Post.class, postId);
        loaded.update("new", "new", new LinkedHashSet<>(List.of("kotlin")));
        entityManager.flush();
        entityManager.clear();

        // then
        Post reloaded = entityManager.find(Post.class, postId);
        assertThat(reloaded.getUpdatedAt())
                .as("@LastModifiedDate는 update flush 시점에 갱신되어야 한다")
                .isAfter(originalUpdatedAt);
        assertThat(reloaded.getTitle()).isEqualTo("new");
        assertThat(reloaded.getContent()).isEqualTo("new");
    }

    @Test
    @DisplayName("update() 후에도 createdAt은 변경되지 않는다")
    void should_keepCreatedAt_when_postModified() throws Exception {
        // given
        User author = persistAuthor();
        Post persisted = entityManager.persistFlushFind(
                Post.create("old", "old", author, Set.of()));
        LocalDateTime originalCreatedAt = persisted.getCreatedAt();
        Long postId = persisted.getId();

        entityManager.clear();
        Thread.sleep(20);

        // when
        Post loaded = entityManager.find(Post.class, postId);
        loaded.update("new", "new", Set.of());
        entityManager.flush();
        entityManager.clear();

        // then
        Post reloaded = entityManager.find(Post.class, postId);
        assertThat(reloaded.getCreatedAt())
                .as("createdAt은 updatable=false이므로 변경되어서는 안 된다")
                .isEqualTo(originalCreatedAt);
    }

    @Test
    @DisplayName("@ElementCollection tags가 정상적으로 영속화되고 다시 조회할 수 있다")
    void should_persistAndLoadTags() {
        // given
        User author = persistAuthor();
        Post post = Post.create("제목", "본문", author,
                new LinkedHashSet<>(List.of("java", "spring")));
        Long postId = entityManager.persistFlushFind(post).getId();

        // when
        entityManager.clear();
        Post reloaded = entityManager.find(Post.class, postId);

        // then
        assertThat(reloaded).isNotNull();
        // LAZY 로딩된 ElementCollection을 실제로 접근해 초기화 트리거
        Set<String> loadedTags = reloaded.getTags();
        assertThat(loadedTags)
                .as("ElementCollection으로 매핑된 태그가 그대로 복원되어야 한다")
                .containsExactlyInAnyOrder("java", "spring");
    }
}
