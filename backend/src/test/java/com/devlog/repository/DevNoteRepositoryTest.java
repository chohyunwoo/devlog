package com.devlog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.devlog.config.JpaAuditingConfig;
import com.devlog.domain.DevNote;
import com.devlog.domain.User;
import java.util.Optional;
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
 * DevNoteRepository 슬라이스 테스트.
 *
 * <p>DevNote 는 "작성자 본인만 접근 가능한" 비공개 엔티티이므로,
 * 이 테스트의 핵심은 repository 메서드가 author 필터를 확실히 걸고 있는지,
 * 즉 타인의 노트가 절대로 유출되지 않는지를 회귀적으로 방어하는 것이다.
 *
 * <p>DevNote/User 엔티티의 @CreatedDate(nullable=false) 때문에 Auditing이
 * 꺼져 있으면 persist 자체가 실패하므로 JpaAuditingConfig를 import 한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class DevNoteRepositoryTest {

    @Autowired
    private DevNoteRepository devNoteRepository;

    @Autowired
    private TestEntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User persistUser(String email, String nickname) {
        return entityManager.persistFlushFind(
                User.create(email, "password1234", nickname, passwordEncoder));
    }

    private DevNote persistDevNote(String title, User author) {
        return entityManager.persistFlushFind(
                DevNote.create(title, "본문-" + title, author));
    }

    @Nested
    @DisplayName("findByAuthor_Id(Long, Pageable)")
    class FindByAuthorId {

        @Test
        @DisplayName("[비공개성 회귀 방어] 본인 author id로 조회하면 본인 노트만 반환되고 타인 노트는 절대 포함되지 않는다")
        void should_returnOnlyOwnNotes_when_queriedByOwnerId() {
            // given
            User userA = persistUser("a@devlog.com", "userA");
            User userB = persistUser("b@devlog.com", "userB");
            persistDevNote("A-1", userA);
            persistDevNote("A-2", userA);
            persistDevNote("A-3", userA);
            persistDevNote("B-1", userB);
            persistDevNote("B-2", userB);

            // when
            Page<DevNote> page = devNoteRepository.findByAuthor_Id(
                    userA.getId(), PageRequest.of(0, 10));

            // then
            assertThat(page.getTotalElements())
                    .as("A의 노트 3개만 세야 한다. 필터가 빠지면 전체 5개가 잡힌다")
                    .isEqualTo(3);
            assertThat(page.getContent())
                    .extracting(n -> n.getAuthor().getId())
                    .as("반환된 모든 노트의 소유자는 오직 A여야 한다")
                    .containsOnly(userA.getId());
            assertThat(page.getContent())
                    .extracting(DevNote::getTitle)
                    .containsExactlyInAnyOrder("A-1", "A-2", "A-3")
                    .doesNotContain("B-1", "B-2");
        }

        @Test
        @DisplayName("페이지 크기와 createdAt DESC 정렬을 적용하면 요청한 만큼만 최신순으로 반환된다")
        void should_returnSortedPage_when_pageableWithSortGiven() throws Exception {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            persistDevNote("n1", author);
            Thread.sleep(5);
            persistDevNote("n2", author);
            Thread.sleep(5);
            persistDevNote("n3", author);
            Thread.sleep(5);
            persistDevNote("n4", author);
            Thread.sleep(5);
            persistDevNote("n5", author);

            // when
            Page<DevNote> page = devNoteRepository.findByAuthor_Id(
                    author.getId(),
                    PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

            // then
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent().get(0).getTitle()).isEqualTo("n5");
            assertThat(page.getContent().get(1).getTitle()).isEqualTo("n4");
        }

        @Test
        @DisplayName("저장된 노트가 있지만 요청한 authorId와 일치하는 게 없으면 빈 페이지를 반환한다")
        void should_returnEmptyPage_when_authorIdNotExists() {
            // given
            User userA = persistUser("a@devlog.com", "userA");
            persistDevNote("A-1", userA);
            persistDevNote("A-2", userA);

            // when
            Page<DevNote> page = devNoteRepository.findByAuthor_Id(
                    9_999_999L, PageRequest.of(0, 10));

            // then
            assertThat(page.isEmpty()).isTrue();
            assertThat(page.getTotalElements()).isZero();
            assertThat(page.getContent()).isEmpty();
        }

        @Test
        @DisplayName("저장된 노트가 전혀 없어도 예외 없이 빈 페이지를 반환한다")
        void should_returnEmptyPage_when_noDevNoteExists() {
            // when
            Page<DevNote> page = devNoteRepository.findByAuthor_Id(
                    1L, Pageable.unpaged());

            // then
            assertThat(page.isEmpty()).isTrue();
            assertThat(page.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("findByIdAndAuthor_Id(Long, Long)")
    class FindByIdAndAuthorId {

        @Test
        @DisplayName("노트의 실제 소유자 id로 조회하면 Optional.of(note)를 반환한다")
        void should_returnPresent_when_ownerMatches() {
            // given
            User userA = persistUser("a@devlog.com", "userA");
            DevNote note = persistDevNote("secret-til", userA);

            // when
            Optional<DevNote> found = devNoteRepository.findByIdAndAuthor_Id(
                    note.getId(), userA.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(note.getId());
            assertThat(found.get().getTitle()).isEqualTo("secret-til");
            assertThat(found.get().getAuthor().getId()).isEqualTo(userA.getId());
        }

        @Test
        @DisplayName("[비공개성 회귀 방어] 소유자가 아닌 유저 id로 같은 노트를 조회하면 Optional.empty - 존재 자체를 은닉해야 한다")
        void should_returnEmpty_when_ownerMismatches() {
            // given
            User userA = persistUser("a@devlog.com", "userA");
            User userB = persistUser("b@devlog.com", "userB");
            DevNote note = persistDevNote("A의 비공개 노트", userA);

            // when
            Optional<DevNote> found = devNoteRepository.findByIdAndAuthor_Id(
                    note.getId(), userB.getId());

            // then
            assertThat(found)
                    .as("author 필터가 빠지면 B가 A의 노트를 열람할 수 있게 된다 - 절대 회귀 금지")
                    .isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 noteId로 조회하면 Optional.empty를 반환한다")
        void should_returnEmpty_when_noteIdNotExists() {
            // given
            User userA = persistUser("a@devlog.com", "userA");
            persistDevNote("A-1", userA);

            // when
            Optional<DevNote> found = devNoteRepository.findByIdAndAuthor_Id(
                    9_999_999L, userA.getId());

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 noteId + 존재하지 않는 authorId로 조회해도 예외 없이 Optional.empty를 반환한다")
        void should_returnEmpty_when_bothNoteIdAndAuthorIdNotExist() {
            // when
            Optional<DevNote> found = devNoteRepository.findByIdAndAuthor_Id(
                    9_999_999L, 8_888_888L);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("save / delete (상속 메서드 smoke)")
    class SaveAndDelete {

        @Test
        @DisplayName("save 후 본인 id로 findByIdAndAuthor_Id 조회하면 동일한 노트가 present")
        void should_beFoundByOwner_when_saved() {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            DevNote note = DevNote.create("saved-note", "본문", author);

            // when
            DevNote saved = devNoteRepository.save(note);
            entityManager.flush();
            entityManager.clear();

            Optional<DevNote> found = devNoteRepository.findByIdAndAuthor_Id(
                    saved.getId(), author.getId());

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("saved-note");
            assertThat(found.get().getContent()).isEqualTo("본문");
        }

        @Test
        @DisplayName("delete 후에는 본인 id로 재조회해도 Optional.empty")
        void should_returnEmpty_when_deleted() {
            // given
            User author = persistUser("a@devlog.com", "authorA");
            DevNote note = persistDevNote("to-be-deleted", author);
            Long noteId = note.getId();

            // when
            devNoteRepository.delete(note);
            entityManager.flush();
            entityManager.clear();

            Optional<DevNote> found = devNoteRepository.findByIdAndAuthor_Id(
                    noteId, author.getId());

            // then
            assertThat(found).isEmpty();
        }
    }
}
