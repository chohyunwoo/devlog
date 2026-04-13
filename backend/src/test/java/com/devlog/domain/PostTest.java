package com.devlog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Post 엔티티의 순수 도메인 로직에 대한 단위 테스트.
 * Spring context 없이 실제 BCryptPasswordEncoder로 준비한 User를 author로 사용한다.
 */
class PostTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User author;

    @BeforeEach
    void setUp() {
        author = User.create("author@devlog.com", "password1234", "author", passwordEncoder);
    }

    @Test
    @DisplayName("create()는 title/content/author를 그대로 세팅하고 태그 순서를 보존한다")
    void should_setFieldsAndPreserveTagOrder_when_created() {
        // given
        String title = "첫 포스트";
        String content = "본문입니다";
        // LinkedHashSet으로 삽입 순서 고정 — Set.of()는 순서 보장이 없음
        Set<String> tags = new LinkedHashSet<>(List.of("java", "spring", "jpa"));

        // when
        Post post = Post.create(title, content, author, tags);

        // then
        assertThat(post.getTitle()).isEqualTo(title);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getAuthor()).isSameAs(author);
        assertThat(post.getTags())
                .as("LinkedHashSet 기반이므로 삽입 순서가 보존되어야 한다")
                .containsExactly("java", "spring", "jpa");
    }

    @Test
    @DisplayName("create()는 빈 태그(Set.of())를 허용한다")
    void should_allowEmptyTags_when_created() {
        // given
        Set<String> noTags = Set.of();

        // when
        Post post = Post.create("제목", "본문", author, noTags);

        // then
        assertThat(post.getTags()).isEmpty();
    }

    @Test
    @DisplayName("create()는 tags가 null이면 NullPointerException을 던진다")
    void should_throwNpe_when_createTagsNull() {
        // given / when / then
        assertThatThrownBy(() -> Post.create("제목", "본문", author, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tags must not be null");
    }

    @Test
    @DisplayName("update()는 title/content/tags를 교체한다")
    void should_replaceFields_when_updated() {
        // given
        Post post = Post.create("old title", "old content", author,
                new LinkedHashSet<>(List.of("java")));

        // when
        post.update("new title", "new content",
                new LinkedHashSet<>(List.of("kotlin")));

        // then
        assertThat(post.getTitle()).isEqualTo("new title");
        assertThat(post.getContent()).isEqualTo("new content");
        assertThat(post.getTags()).containsExactly("kotlin");
    }

    @Test
    @DisplayName("update()는 tags가 null이면 NullPointerException을 던진다")
    void should_throwNpe_when_updateTagsNull() {
        // given
        Post post = Post.create("제목", "본문", author, Set.of());

        // when / then
        assertThatThrownBy(() -> post.update("t", "c", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tags must not be null");
    }

    @Test
    @DisplayName("update() 호출 전후에도 author는 동일한 인스턴스로 유지된다")
    void should_keepAuthor_when_updated() {
        // given
        Post post = Post.create("제목", "본문", author, Set.of());
        User before = post.getAuthor();

        // when
        post.update("new", "new", Set.of());

        // then
        assertThat(post.getAuthor())
                .as("author는 updatable=false이므로 update로 바뀌어서는 안 된다")
                .isSameAs(before);
    }

    @Test
    @DisplayName("isAuthoredBy()는 실제 작성자 id에는 true, 다른 id에는 false를 반환한다")
    void should_returnTrueOnlyForOwningUserId() throws Exception {
        // given
        // author.id는 자동 생성되므로 reflection으로 세팅해 isAuthoredBy의 동작만 격리해서 검증한다.
        setUserId(author, 42L);
        Post post = Post.create("제목", "본문", author, Set.of());

        // when / then
        assertThat(post.isAuthoredBy(42L)).isTrue();
        assertThat(post.isAuthoredBy(99L)).isFalse();
    }

    @Test
    @DisplayName("getTags()가 반환한 Set은 수정할 수 없다")
    void should_returnUnmodifiableTags() {
        // given
        Post post = Post.create("제목", "본문", author,
                new LinkedHashSet<>(List.of("java")));

        // when
        Set<String> tags = post.getTags();

        // then
        assertThatThrownBy(() -> tags.add("spring"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> tags.remove("java"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("중복 태그가 섞여 있어도 Set 동작에 의해 중복이 제거된다")
    void should_deduplicateTags_when_inputContainsDuplicates() {
        // given
        // Set.of("a","b","a")는 IllegalArgumentException을 던지므로
        // 중복을 의도적으로 담기 위해 LinkedHashSet(List.of(...))를 사용한다.
        Set<String> withDuplicates = new LinkedHashSet<>(List.of("a", "b", "a"));

        // when
        Post post = Post.create("제목", "본문", author, withDuplicates);

        // then
        assertThat(post.getTags())
                .as("Set 기반이므로 'a'는 한 번만 들어가야 한다")
                .hasSize(2)
                .containsExactly("a", "b");
    }

    private static void setUserId(User user, Long id) throws Exception {
        java.lang.reflect.Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
    }
}
