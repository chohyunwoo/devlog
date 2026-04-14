package com.devlog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.devlog.config.JpaAuditingConfig;
import com.devlog.domain.Post;
import com.devlog.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PostRepository 슬라이스 테스트.
 *
 * <p>@EntityGraph({"author"}) / JOIN FETCH / MEMBER OF 기반 쿼리와
 * 페이지네이션·countQuery·소유권 검증이 실제 H2(MySQL 모드) 위에서
 * 의도대로 동작하는지, 그리고 fetch 전략이 회귀되지 않는지를 검증한다.
 *
 * <p>Post/User 엔티티의 @CreatedDate(nullable=false) 때문에 Auditing이
 * 꺼져 있으면 persist 자체가 실패하므로 JpaAuditingConfig를 import 한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManager em;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User persistUser(String email, String nickname) {
        return entityManager.persistFlushFind(
                User.create(email, "password1234", nickname, passwordEncoder));
    }

    private Post persistPost(String title, User author, Set<String> tags) {
        return entityManager.persistFlushFind(
                Post.create(title, "본문-" + title, author, tags));
    }

    private Post persistPost(String title, User author) {
        return persistPost(title, author, Set.of());
    }

    @Nested
    @DisplayName("findAll(Pageable)")
    class FindAll {

        @Test
        @DisplayName("페이지 크기와 정렬을 적용하면 요청한 만큼만 최신순으로 반환된다")
        void should_returnSortedPage_when_pageableWithSortGiven() throws Exception {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            persistPost("p1", author);
            Thread.sleep(5);
            persistPost("p2", author);
            Thread.sleep(5);
            persistPost("p3", author);
            Thread.sleep(5);
            persistPost("p4", author);
            Thread.sleep(5);
            persistPost("p5", author);

            // when
            Page<Post> page = postRepository.findAll(
                    PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

            // then
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent().get(0).getTitle()).isEqualTo("p5");
            assertThat(page.getContent().get(1).getTitle()).isEqualTo("p4");
        }

        @Test
        @DisplayName("저장된 Post가 하나도 없으면 빈 페이지를 반환한다")
        void should_returnEmptyPage_when_noPostExists() {
            // when
            Page<Post> page = postRepository.findAll(Pageable.unpaged());

            // then
            assertThat(page.isEmpty()).isTrue();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("@EntityGraph에 의해 author가 즉시 로딩되어 있다")
        void should_fetchAuthor_when_findAllCalled() {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            persistPost("p1", author);
            entityManager.flush();
            entityManager.clear();

            // when
            Page<Post> page = postRepository.findAll(PageRequest.of(0, 10));

            // then
            assertThat(page.getContent()).hasSize(1);
            Post loaded = page.getContent().get(0);
            PersistenceUnitUtil unitUtil = em.getEntityManagerFactory().getPersistenceUnitUtil();
            assertThat(unitUtil.isLoaded(loaded, "author"))
                    .as("@EntityGraph(attributePaths={\"author\"})가 빠지면 LAZY 프록시로 남아 false가 된다")
                    .isTrue();
            // 실제로 필드 접근까지 문제없는지도 확인
            assertThat(loaded.getAuthor().getEmail()).isEqualTo("a@devlog.com");
        }

        @Test
        @DisplayName("@EntityGraph에 의해 tags도 즉시 로딩되어 트랜잭션 밖에서도 접근할 수 있다")
        void should_fetchTags_when_findAllCalled() {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            persistPost("p1", author, new LinkedHashSet<>(List.of("java", "spring")));
            entityManager.flush();
            entityManager.clear();

            // when
            Page<Post> page = postRepository.findAll(PageRequest.of(0, 10));

            // then
            Post loaded = page.getContent().get(0);
            PersistenceUnitUtil unitUtil = em.getEntityManagerFactory().getPersistenceUnitUtil();
            assertThat(unitUtil.isLoaded(loaded, "tags"))
                    .as("open-in-view=false 환경에서 tags 가 LAZY 로 남으면 "
                            + "컨트롤러 렌더링 단계에서 LazyInitializationException 이 발생한다")
                    .isTrue();
            assertThat(loaded.getTags()).containsExactlyInAnyOrder("java", "spring");
        }
    }

    @Nested
    @DisplayName("findByAuthor_Id(Long, Pageable)")
    class FindByAuthorId {

        @Test
        @DisplayName("특정 유저의 Post만 필터링되어 반환된다")
        void should_returnOnlyPostsOfGivenAuthor() {
            // given
            User userA = persistUser("a@devlog.com", "userA");
            User userB = persistUser("b@devlog.com", "userB");
            persistPost("A-1", userA);
            persistPost("A-2", userA);
            persistPost("B-1", userB);
            persistPost("B-2", userB);

            // when
            Page<Post> page = postRepository.findByAuthor_Id(
                    userA.getId(), PageRequest.of(0, 10));

            // then
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(p -> p.getAuthor().getId())
                    .containsOnly(userA.getId());
            assertThat(page.getContent())
                    .extracting(Post::getTitle)
                    .containsExactlyInAnyOrder("A-1", "A-2");
        }

        @Test
        @DisplayName("존재하지 않는 authorId로 조회하면 빈 페이지를 반환한다")
        void should_returnEmptyPage_when_authorIdNotExists() {
            // given
            User userA = persistUser("a@devlog.com", "userA");
            persistPost("A-1", userA);

            // when
            Page<Post> page = postRepository.findByAuthor_Id(9_999_999L, PageRequest.of(0, 10));

            // then
            assertThat(page.isEmpty()).isTrue();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("author 와 tags 가 함께 즉시 로딩된다")
        void should_fetchAuthorAndTags_when_findByAuthorIdCalled() {
            // given
            User userA = persistUser("a@devlog.com", "userA");
            persistPost("A-1", userA, new LinkedHashSet<>(List.of("java")));
            entityManager.flush();
            entityManager.clear();

            // when
            Page<Post> page = postRepository.findByAuthor_Id(
                    userA.getId(), PageRequest.of(0, 10));

            // then
            Post loaded = page.getContent().get(0);
            PersistenceUnitUtil unitUtil = em.getEntityManagerFactory().getPersistenceUnitUtil();
            assertThat(unitUtil.isLoaded(loaded, "author")).isTrue();
            assertThat(unitUtil.isLoaded(loaded, "tags")).isTrue();
            assertThat(loaded.getTags()).containsExactly("java");
        }
    }

    @Nested
    @DisplayName("findByTag(String, Pageable)")
    class FindByTag {

        @Test
        @DisplayName("지정한 태그를 포함하는 Post만 반환한다")
        void should_returnPostsHavingGivenTag() {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            persistPost("p1", author, new LinkedHashSet<>(List.of("java", "spring")));
            persistPost("p2", author, new LinkedHashSet<>(List.of("kotlin")));
            persistPost("p3", author, new LinkedHashSet<>(List.of("java")));

            // when
            Page<Post> page = postRepository.findByTag("java", PageRequest.of(0, 10));

            // then
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(Post::getTitle)
                    .containsExactlyInAnyOrder("p1", "p3");
        }

        @Test
        @DisplayName("어떤 Post에도 없는 태그로 조회하면 빈 페이지를 반환한다")
        void should_returnEmptyPage_when_tagNotUsed() {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            persistPost("p1", author, new LinkedHashSet<>(List.of("java")));

            // when
            Page<Post> page = postRepository.findByTag("python", PageRequest.of(0, 10));

            // then
            assertThat(page.isEmpty()).isTrue();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("반환된 Post 의 tags 는 즉시 로딩되어 있다")
        void should_fetchTags_when_findByTagCalled() {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            persistPost("p1", author, new LinkedHashSet<>(List.of("java", "spring")));
            entityManager.flush();
            entityManager.clear();

            // when
            Page<Post> page = postRepository.findByTag("java", PageRequest.of(0, 10));

            // then
            Post loaded = page.getContent().get(0);
            PersistenceUnitUtil unitUtil = em.getEntityManagerFactory().getPersistenceUnitUtil();
            assertThat(unitUtil.isLoaded(loaded, "author")).isTrue();
            assertThat(unitUtil.isLoaded(loaded, "tags")).isTrue();
            assertThat(loaded.getTags()).containsExactlyInAnyOrder("java", "spring");
        }

        @Test
        @DisplayName("countQuery가 태그 필터를 반영해 totalElements/totalPages를 정확히 계산한다")
        void should_computeTotalsCorrectly_when_countQueryApplied() {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            // 10개 중 5개만 "test" 태그를 가짐
            for (int i = 1; i <= 5; i++) {
                persistPost("tagged-" + i, author, new LinkedHashSet<>(List.of("test")));
            }
            for (int i = 1; i <= 5; i++) {
                persistPost("plain-" + i, author, new LinkedHashSet<>(List.of("other")));
            }

            // when
            Page<Post> page = postRepository.findByTag("test", PageRequest.of(0, 2));

            // then
            assertThat(page.getTotalElements())
                    .as("countQuery가 전체 Post(10)가 아니라 tag=test인 5개만 세야 한다")
                    .isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
            assertThat(page.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findDetailById(Long)")
    class FindDetailById {

        @Test
        @DisplayName("정상 조회 시 JOIN FETCH로 author가 즉시 로딩된 Post를 반환한다")
        void should_returnPostWithFetchedAuthor_when_idExists() {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            Post saved = persistPost("p1", author);
            Long postId = saved.getId();
            entityManager.flush();
            entityManager.clear();

            // when
            Optional<Post> found = postRepository.findDetailById(postId);

            // then
            assertThat(found).isPresent();
            Post loaded = found.get();
            PersistenceUnitUtil unitUtil = em.getEntityManagerFactory().getPersistenceUnitUtil();
            assertThat(unitUtil.isLoaded(loaded, "author"))
                    .as("JOIN FETCH p.author가 빠지면 author가 LAZY 프록시로 남아 false가 된다")
                    .isTrue();
            assertThat(loaded.getAuthor().getEmail()).isEqualTo("a@devlog.com");
            assertThat(loaded.getTitle()).isEqualTo("p1");
        }

        @Test
        @DisplayName("존재하지 않는 id로 조회하면 Optional.empty()를 반환한다")
        void should_returnEmpty_when_idNotExists() {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            persistPost("p1", author);

            // when
            Optional<Post> found = postRepository.findDetailById(9_999_999L);

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("author 와 tags 가 함께 즉시 로딩된 Post 를 반환한다")
        void should_fetchAuthorAndTags_when_findDetailByIdCalled() {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            Post saved = persistPost("p1", author, new LinkedHashSet<>(List.of("java", "spring")));
            Long postId = saved.getId();
            entityManager.flush();
            entityManager.clear();

            // when
            Optional<Post> found = postRepository.findDetailById(postId);

            // then
            assertThat(found).isPresent();
            Post loaded = found.get();
            PersistenceUnitUtil unitUtil = em.getEntityManagerFactory().getPersistenceUnitUtil();
            assertThat(unitUtil.isLoaded(loaded, "author")).isTrue();
            assertThat(unitUtil.isLoaded(loaded, "tags"))
                    .as("JOIN FETCH p.tags 가 빠지면 컨트롤러가 태그 직렬화 시점에 폭발한다")
                    .isTrue();
            assertThat(loaded.getTags()).containsExactlyInAnyOrder("java", "spring");
        }
    }

    @Nested
    @DisplayName("existsByIdAndAuthor_Id(Long, Long)")
    class ExistsByIdAndAuthorId {

        @Test
        @DisplayName("Post의 실제 소유자 id로 조회하면 true를 반환한다")
        void should_returnTrue_when_ownerMatches() {
            // given
            User userA = persistUser("a@devlog.com", "userA");
            Post post = persistPost("p1", userA);

            // when
            boolean exists = postRepository.existsByIdAndAuthor_Id(post.getId(), userA.getId());

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Post의 소유자가 아닌 유저 id로 조회하면 false를 반환한다")
        void should_returnFalse_when_ownerMismatches() {
            // given
            User userA = persistUser("a@devlog.com", "userA");
            User userB = persistUser("b@devlog.com", "userB");
            Post post = persistPost("p1", userA);

            // when
            boolean exists = postRepository.existsByIdAndAuthor_Id(post.getId(), userB.getId());

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 postId로 조회하면 false를 반환한다")
        void should_returnFalse_when_postIdNotExists() {
            // given
            User userA = persistUser("a@devlog.com", "userA");
            persistPost("p1", userA);

            // when
            boolean exists = postRepository.existsByIdAndAuthor_Id(9_999_999L, userA.getId());

            // then
            assertThat(exists).isFalse();
        }
    }
}
