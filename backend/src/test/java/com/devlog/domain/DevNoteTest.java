package com.devlog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DevNote 엔티티의 순수 도메인 로직에 대한 단위 테스트.
 * Spring context 없이 실제 BCryptPasswordEncoder로 준비한 User를 author로 사용한다.
 * createdAt/updatedAt 등 auditing 관련 동작은 DevNoteAuditingTest에서 검증한다.
 */
class DevNoteTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User author;

    @BeforeEach
    void setUp() {
        author = User.create("author@devlog.com", "password1234", "author", passwordEncoder);
    }

    @Test
    @DisplayName("create()는 title/content/author를 그대로 세팅한다")
    void should_setFields_when_created() {
        // given
        String title = "오늘의 TIL";
        String content = "JPA Auditing을 공부했다";

        // when
        DevNote devNote = DevNote.create(title, content, author);

        // then
        assertThat(devNote.getTitle()).isEqualTo(title);
        assertThat(devNote.getContent()).isEqualTo(content);
        assertThat(devNote.getAuthor()).isSameAs(author);
    }

    @Test
    @DisplayName("update()는 title/content를 교체한다")
    void should_replaceFields_when_updated() {
        // given
        DevNote devNote = DevNote.create("old title", "old content", author);

        // when
        devNote.update("new title", "new content");

        // then
        assertThat(devNote.getTitle()).isEqualTo("new title");
        assertThat(devNote.getContent()).isEqualTo("new content");
    }

    @Test
    @DisplayName("update() 호출 전후에도 author는 동일한 인스턴스로 유지된다")
    void should_keepAuthor_when_updated() {
        // given
        DevNote devNote = DevNote.create("제목", "본문", author);
        User before = devNote.getAuthor();

        // when
        devNote.update("new", "new");

        // then
        assertThat(devNote.getAuthor())
                .as("author는 updatable=false이므로 update로 바뀌어서는 안 된다")
                .isSameAs(before);
    }

    @Test
    @DisplayName("isAuthoredBy()는 실제 작성자 id에는 true, 다른 id에는 false를 반환한다")
    void should_returnTrueOnlyForOwningUserId() throws Exception {
        // given
        // author.id는 자동 생성되므로 reflection으로 세팅해 isAuthoredBy의 동작만 격리해서 검증한다.
        setUserId(author, 42L);
        DevNote devNote = DevNote.create("제목", "본문", author);

        // when / then
        assertThat(devNote.isAuthoredBy(42L)).isTrue();
        assertThat(devNote.isAuthoredBy(99L)).isFalse();
    }

    private static void setUserId(User user, Long id) throws Exception {
        java.lang.reflect.Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
    }
}
