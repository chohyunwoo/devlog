package com.devlog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.devlog.common.exception.PostAccessDeniedException;
import com.devlog.common.exception.PostNotFoundException;
import com.devlog.controller.dto.PostCreateRequest;
import com.devlog.controller.dto.PostUpdateRequest;
import com.devlog.domain.Post;
import com.devlog.domain.User;
import com.devlog.repository.PostRepository;
import com.devlog.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PostService 단위 테스트.
 *
 * 설계 메모
 * - 순수 단위 테스트(Mockito). Spring context 없이 빠르게 CRUD/쿼리 위임을 검증한다.
 * - Post 는 대부분의 케이스에서 실제 객체(User.create + Post.create)를 사용한다.
 *   {@code isAuthoredBy} 의 실제 분기를 검증하기 위함이며, id 는 reflection 으로 세팅한다.
 * - update/delete 의 "소유자 아님" 케이스는 분기 자체가 목적이므로 {@code mock(Post.class)} 를 사용해
 *   update/saveAndFlush/delete 가 호출되지 않았음을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 단위 테스트")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    private static final Long AUTHOR_ID = 42L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long POST_ID = 7L;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // --------- 테스트 픽스처 헬퍼 --------- //

    private User buildAuthor(Long id) {
        User user = User.create("author@devlog.com", "password1234", "author", passwordEncoder);
        setField(User.class, user, "id", id);
        return user;
    }

    private Post buildPersistedPost(Long postId, User author, Set<String> tags) {
        Post post = Post.create("title", "content", author, tags);
        setField(Post.class, post, "id", postId);
        setField(Post.class, post, "createdAt", LocalDateTime.of(2026, 4, 13, 10, 0, 0));
        setField(Post.class, post, "updatedAt", LocalDateTime.of(2026, 4, 13, 10, 0, 0));
        return post;
    }

    private static void setField(Class<?> clazz, Object target, String fieldName, Object value) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("failed to set field: " + fieldName, e);
        }
    }

    // =====================================================================
    // 1. create
    // =====================================================================

    @Nested
    @DisplayName("create")
    class Create {

        /**
         * save() 가 ID 를 할당하고, 이어지는 findDetailById(ID) 가 같은 Post 를 돌려주도록
         * 스텁한다. 실제 JPA 환경에서는 save 이후 재조회가 EntityGraph 경로로 author/tags 를
         * 즉시 로딩한 상태를 보장한다 — 단위 테스트에서는 동일 인스턴스 반환으로 충분하다.
         */
        private AtomicReference<Post> stubSaveAndFindDetail() {
            AtomicReference<Post> savedRef = new AtomicReference<>();
            given(postRepository.save(any(Post.class))).willAnswer(inv -> {
                Post p = inv.getArgument(0);
                setField(Post.class, p, "id", POST_ID);
                savedRef.set(p);
                return p;
            });
            given(postRepository.findDetailById(POST_ID))
                    .willAnswer(inv -> Optional.ofNullable(savedRef.get()));
            return savedRef;
        }

        @Test
        @DisplayName("정상 생성 — save 후 findDetailById 로 재조회해 author/tags 가 로딩된 Post 를 반환한다")
        void should_createAndSavePost_when_requestIsValid() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Set<String> tags = new LinkedHashSet<>(List.of("java", "spring"));
            PostCreateRequest req = new PostCreateRequest("title", "content", tags);

            given(userRepository.getReferenceById(AUTHOR_ID)).willReturn(author);
            stubSaveAndFindDetail();

            // when
            Post result = postService.create(AUTHOR_ID, req);

            // then: 반환된 Post 의 필드 값
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("title");
            assertThat(result.getContent()).isEqualTo("content");
            assertThat(result.getAuthor()).isSameAs(author);
            assertThat(result.getTags()).containsExactlyInAnyOrder("java", "spring");

            // then: save 에 전달된 Post 의 tags 가 request 값 그대로인지 검증
            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            assertThat(captor.getValue().getTags())
                    .containsExactlyInAnyOrder("java", "spring");

            verify(userRepository).getReferenceById(AUTHOR_ID);
            verify(postRepository).findDetailById(POST_ID);
        }

        @Test
        @DisplayName("tags 가 null 이면 빈 Set 으로 정규화해 Post 를 생성한다")
        void should_normalizeNullTagsToEmptySet_when_tagsIsNull() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            PostCreateRequest req = new PostCreateRequest("title", "content", null);
            given(userRepository.getReferenceById(AUTHOR_ID)).willReturn(author);
            stubSaveAndFindDetail();

            // when
            Post result = postService.create(AUTHOR_ID, req);

            // then: 빈 Set (non-null) 으로 정규화되어 Post.create 에 전달됨
            assertThat(result.getTags()).isEmpty();
            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            assertThat(captor.getValue().getTags()).isEmpty();
        }

        @Test
        @DisplayName("tags 값이 있으면 Set.copyOf 로 복사되어 Post.create 에 전달된다")
        void should_copyTags_when_tagsIsProvided() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Set<String> originalTags = new LinkedHashSet<>(List.of("kotlin"));
            PostCreateRequest req = new PostCreateRequest("title", "content", originalTags);
            given(userRepository.getReferenceById(AUTHOR_ID)).willReturn(author);
            stubSaveAndFindDetail();

            // when
            postService.create(AUTHOR_ID, req);

            // then: Post 내부 tags 가 원본 Set 과 같은 값을 포함하지만 동일 인스턴스는 아님
            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            assertThat(captor.getValue().getTags()).containsExactly("kotlin");
            // Post 내부에서 새 LinkedHashSet 으로 복사하므로 참조 동일성은 보장되지 않아야 함
            assertThat(captor.getValue().getTags()).isNotSameAs(originalTags);
        }

        @Test
        @DisplayName("save 직후 findDetailById 가 비어 있으면 PostNotFoundException 을 던진다")
        void should_throwPostNotFound_when_postDisappearsRightAfterSave() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            PostCreateRequest req = new PostCreateRequest("t", "c", Set.of());
            given(userRepository.getReferenceById(AUTHOR_ID)).willReturn(author);
            given(postRepository.save(any(Post.class))).willAnswer(inv -> {
                Post p = inv.getArgument(0);
                setField(Post.class, p, "id", POST_ID);
                return p;
            });
            given(postRepository.findDetailById(POST_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> postService.create(AUTHOR_ID, req))
                    .isInstanceOf(PostNotFoundException.class);
        }
    }

    // =====================================================================
    // 2. update
    // =====================================================================

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("정상 업데이트 — 작성자 본인이면 Post.update 를 호출하고 saveAndFlush 한다")
        void should_updateAndSaveAndFlush_when_requesterIsAuthor() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, Set.of("old"));
            PostUpdateRequest req = new PostUpdateRequest("new title", "new content", Set.of("new"));

            given(postRepository.findDetailById(POST_ID)).willReturn(Optional.of(post));
            given(postRepository.saveAndFlush(post)).willReturn(post);

            // when
            Post result = postService.update(POST_ID, AUTHOR_ID, req);

            // then: Post 상태가 실제로 업데이트되었는지
            assertThat(result).isSameAs(post);
            assertThat(result.getTitle()).isEqualTo("new title");
            assertThat(result.getContent()).isEqualTo("new content");
            assertThat(result.getTags()).containsExactly("new");

            verify(postRepository).findDetailById(POST_ID);
            verify(postRepository).saveAndFlush(post);
        }

        @Test
        @DisplayName("tags 가 null 이면 빈 Set 으로 정규화해 Post.update 에 전달한다")
        void should_normalizeNullTagsToEmpty_when_updateTagsIsNull() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, Set.of("old"));
            PostUpdateRequest req = new PostUpdateRequest("new title", "new content", null);

            given(postRepository.findDetailById(POST_ID)).willReturn(Optional.of(post));
            given(postRepository.saveAndFlush(post)).willReturn(post);

            // when
            Post result = postService.update(POST_ID, AUTHOR_ID, req);

            // then
            assertThat(result.getTags()).isEmpty();
        }

        @Test
        @DisplayName("Post 가 없으면 PostNotFoundException 을 던지고 saveAndFlush 는 호출되지 않는다")
        void should_throwPostNotFound_when_postDoesNotExist() {
            // given
            PostUpdateRequest req = new PostUpdateRequest("t", "c", Set.of());
            given(postRepository.findDetailById(POST_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> postService.update(POST_ID, AUTHOR_ID, req))
                    .isInstanceOf(PostNotFoundException.class);

            verify(postRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("작성자가 아니면 PostAccessDeniedException 을 던지고 update/saveAndFlush 는 호출되지 않는다")
        void should_throwPostAccessDenied_when_requesterIsNotAuthor() {
            // given: isAuthoredBy 호출 여부 자체를 검증하기 위해 mock Post 사용
            Post post = mock(Post.class);
            given(postRepository.findDetailById(POST_ID)).willReturn(Optional.of(post));
            given(post.isAuthoredBy(OTHER_USER_ID)).willReturn(false);

            PostUpdateRequest req = new PostUpdateRequest("t", "c", Set.of());

            // when / then
            assertThatThrownBy(() -> postService.update(POST_ID, OTHER_USER_ID, req))
                    .isInstanceOf(PostAccessDeniedException.class);

            verify(post, never()).update(any(), any(), any());
            verify(postRepository, never()).saveAndFlush(any());
        }
    }

    // =====================================================================
    // 3. delete
    // =====================================================================

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("정상 삭제 — 작성자 본인이면 postRepository.delete(post) 를 호출한다")
        void should_deletePost_when_requesterIsAuthor() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, Set.of());
            given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));

            // when
            postService.delete(POST_ID, AUTHOR_ID);

            // then
            verify(postRepository).delete(post);
        }

        @Test
        @DisplayName("Post 가 없으면 PostNotFoundException 을 던지고 delete 는 호출되지 않는다")
        void should_throwPostNotFound_when_postDoesNotExist() {
            // given
            given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> postService.delete(POST_ID, AUTHOR_ID))
                    .isInstanceOf(PostNotFoundException.class);

            verify(postRepository, never()).delete(any(Post.class));
        }

        @Test
        @DisplayName("작성자가 아니면 PostAccessDeniedException 을 던지고 delete 는 호출되지 않는다")
        void should_throwPostAccessDenied_when_requesterIsNotAuthor() {
            // given
            Post post = mock(Post.class);
            given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
            given(post.isAuthoredBy(OTHER_USER_ID)).willReturn(false);

            // when / then
            assertThatThrownBy(() -> postService.delete(POST_ID, OTHER_USER_ID))
                    .isInstanceOf(PostAccessDeniedException.class);

            verify(postRepository, never()).delete(any(Post.class));
        }
    }

    // =====================================================================
    // 4. findDetail
    // =====================================================================

    @Nested
    @DisplayName("findDetail")
    class FindDetail {

        @Test
        @DisplayName("정상 조회 — findDetailById 결과를 그대로 반환한다")
        void should_returnPost_when_postExists() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, Set.of("java"));
            given(postRepository.findDetailById(POST_ID)).willReturn(Optional.of(post));

            // when
            Post result = postService.findDetail(POST_ID);

            // then
            assertThat(result).isSameAs(post);
            verify(postRepository).findDetailById(POST_ID);
        }

        @Test
        @DisplayName("Post 가 없으면 PostNotFoundException 을 던진다")
        void should_throwPostNotFound_when_postDoesNotExist() {
            // given
            given(postRepository.findDetailById(POST_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> postService.findDetail(POST_ID))
                    .isInstanceOf(PostNotFoundException.class);
        }
    }

    // =====================================================================
    // 5. findAll / findByAuthor / findByTag (Repository 위임)
    // =====================================================================

    @Nested
    @DisplayName("조회 위임")
    class QueryDelegation {

        private final Pageable pageable = PageRequest.of(0, 20);

        @Test
        @DisplayName("findAll 은 postRepository.findAll(pageable) 을 호출하고 결과를 그대로 반환한다")
        void should_delegateToRepositoryFindAll() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, Set.of());
            Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
            given(postRepository.findAll(pageable)).willReturn(page);

            // when
            Page<Post> result = postService.findAll(pageable);

            // then
            assertThat(result).isSameAs(page);
            verify(postRepository).findAll(pageable);
        }

        @Test
        @DisplayName("findByAuthor 는 postRepository.findByAuthor_Id(authorId, pageable) 를 호출하고 결과를 그대로 반환한다")
        void should_delegateToRepositoryFindByAuthor() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, Set.of());
            Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
            given(postRepository.findByAuthor_Id(AUTHOR_ID, pageable)).willReturn(page);

            // when
            Page<Post> result = postService.findByAuthor(AUTHOR_ID, pageable);

            // then
            assertThat(result).isSameAs(page);
            verify(postRepository).findByAuthor_Id(eq(AUTHOR_ID), eq(pageable));
        }

        @Test
        @DisplayName("findByTag 는 postRepository.findByTag(tag, pageable) 을 호출하고 결과를 그대로 반환한다")
        void should_delegateToRepositoryFindByTag() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, Set.of("java"));
            Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
            given(postRepository.findByTag("java", pageable)).willReturn(page);

            // when
            Page<Post> result = postService.findByTag("java", pageable);

            // then
            assertThat(result).isSameAs(page);
            verify(postRepository).findByTag(eq("java"), eq(pageable));
        }
    }
}
