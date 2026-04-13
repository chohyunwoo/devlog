package com.devlog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.devlog.common.exception.DevNoteNotFoundException;
import com.devlog.controller.dto.DevNoteCreateRequest;
import com.devlog.controller.dto.DevNoteUpdateRequest;
import com.devlog.domain.DevNote;
import com.devlog.domain.User;
import com.devlog.repository.DevNoteRepository;
import com.devlog.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
 * DevNoteService 단위 테스트.
 *
 * 설계 메모
 * - 순수 단위 테스트(Mockito). Spring context 없이 빠르게 CRUD/쿼리 위임을 검증한다.
 * - DevNote 는 비공개 엔티티이므로 PostService 와 달리 "타인 소유"와 "미존재"가
 *   하나의 분기({@code findByIdAndAuthor_Id} → empty)로 합쳐진다. 즉 403 분기가 없고,
 *   소유권 검증은 리포지토리 쿼리 자체가 책임진다.
 * - 정상 케이스는 실제 DevNote.create + reflection id 세팅을 사용한다.
 * - update 의 "내부 update 호출" 검증은 mock(DevNote.class) 를 사용해 호출 여부를 직접 본다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DevNoteService 단위 테스트")
class DevNoteServiceTest {

    @Mock
    private DevNoteRepository devNoteRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DevNoteService devNoteService;

    private static final Long AUTHOR_ID = 42L;
    private static final Long NOTE_ID = 7L;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // --------- 테스트 픽스처 헬퍼 --------- //

    private User buildAuthor(Long id) {
        User user = User.create("author@devlog.com", "password1234", "author", passwordEncoder);
        setField(User.class, user, "id", id);
        return user;
    }

    private DevNote buildPersistedDevNote(Long noteId, User author, String title, String content) {
        DevNote devNote = DevNote.create(title, content, author);
        setField(DevNote.class, devNote, "id", noteId);
        setField(DevNote.class, devNote, "createdAt", LocalDateTime.of(2026, 4, 13, 10, 0, 0));
        setField(DevNote.class, devNote, "updatedAt", LocalDateTime.of(2026, 4, 13, 10, 0, 0));
        return devNote;
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

        @Test
        @DisplayName("정상 생성 — author proxy 를 getReferenceById 로 얻어 DevNote 를 저장하고 결과를 반환한다")
        void should_createAndSaveDevNote_when_requestIsValid() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            DevNoteCreateRequest req = new DevNoteCreateRequest("title", "content");

            given(userRepository.getReferenceById(AUTHOR_ID)).willReturn(author);
            given(devNoteRepository.save(any(DevNote.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            DevNote result = devNoteService.create(AUTHOR_ID, req);

            // then: 반환된 DevNote 의 필드 값
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("title");
            assertThat(result.getContent()).isEqualTo("content");
            assertThat(result.getAuthor()).isSameAs(author);

            // then: save 에 전달된 DevNote 가 request 값 그대로 매핑되었는지 검증
            ArgumentCaptor<DevNote> captor = ArgumentCaptor.forClass(DevNote.class);
            verify(devNoteRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("title");
            assertThat(captor.getValue().getContent()).isEqualTo("content");
            assertThat(captor.getValue().getAuthor()).isSameAs(author);

            verify(userRepository).getReferenceById(AUTHOR_ID);
        }
    }

    // =====================================================================
    // 2. findDetail
    // =====================================================================

    @Nested
    @DisplayName("findDetail")
    class FindDetail {

        @Test
        @DisplayName("정상 조회 — findByIdAndAuthor_Id 결과를 그대로 반환한다")
        void should_returnDevNote_when_devNoteExistsForAuthor() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            DevNote devNote = buildPersistedDevNote(NOTE_ID, author, "title", "content");
            given(devNoteRepository.findByIdAndAuthor_Id(NOTE_ID, AUTHOR_ID))
                    .willReturn(Optional.of(devNote));

            // when
            DevNote result = devNoteService.findDetail(NOTE_ID, AUTHOR_ID);

            // then
            assertThat(result).isSameAs(devNote);
            verify(devNoteRepository).findByIdAndAuthor_Id(NOTE_ID, AUTHOR_ID);
        }

        @Test
        @DisplayName("미존재 또는 타인 소유면 DevNoteNotFoundException 을 던진다 (403 분기 없음)")
        void should_throwDevNoteNotFound_when_devNoteDoesNotExistOrOwnedByOther() {
            // given: findByIdAndAuthor_Id 가 author 필터를 강제하므로
            //        "다른 사용자 소유"와 "미존재"는 둘 다 Optional.empty 로 합쳐진다.
            given(devNoteRepository.findByIdAndAuthor_Id(NOTE_ID, AUTHOR_ID))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> devNoteService.findDetail(NOTE_ID, AUTHOR_ID))
                    .isInstanceOf(DevNoteNotFoundException.class);
        }
    }

    // =====================================================================
    // 3. findByAuthor
    // =====================================================================

    @Nested
    @DisplayName("findByAuthor")
    class FindByAuthor {

        private final Pageable pageable = PageRequest.of(0, 20);

        @Test
        @DisplayName("devNoteRepository.findByAuthor_Id(authorId, pageable) 로 위임하고 결과를 그대로 반환한다")
        void should_delegateToRepositoryFindByAuthor() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            DevNote devNote = buildPersistedDevNote(NOTE_ID, author, "t1", "c1");
            Page<DevNote> page = new PageImpl<>(List.of(devNote), pageable, 1);
            given(devNoteRepository.findByAuthor_Id(AUTHOR_ID, pageable)).willReturn(page);

            // when
            Page<DevNote> result = devNoteService.findByAuthor(AUTHOR_ID, pageable);

            // then
            assertThat(result).isSameAs(page);
            verify(devNoteRepository).findByAuthor_Id(eq(AUTHOR_ID), eq(pageable));
        }
    }

    // =====================================================================
    // 4. update
    // =====================================================================

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("정상 업데이트 — 본인 소유 노트면 DevNote.update 를 호출하고 saveAndFlush 한다")
        void should_updateAndSaveAndFlush_when_devNoteExistsForAuthor() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            DevNote devNote = buildPersistedDevNote(NOTE_ID, author, "old title", "old content");
            DevNoteUpdateRequest req = new DevNoteUpdateRequest("new title", "new content");

            given(devNoteRepository.findByIdAndAuthor_Id(NOTE_ID, AUTHOR_ID))
                    .willReturn(Optional.of(devNote));
            given(devNoteRepository.saveAndFlush(devNote)).willReturn(devNote);

            // when
            DevNote result = devNoteService.update(NOTE_ID, AUTHOR_ID, req);

            // then: DevNote 상태가 실제로 업데이트되었는지
            assertThat(result).isSameAs(devNote);
            assertThat(result.getTitle()).isEqualTo("new title");
            assertThat(result.getContent()).isEqualTo("new content");

            verify(devNoteRepository).findByIdAndAuthor_Id(NOTE_ID, AUTHOR_ID);
            verify(devNoteRepository).saveAndFlush(devNote);
        }

        @Test
        @DisplayName("미존재 또는 타인 소유면 DevNoteNotFoundException 을 던지고 update/saveAndFlush 는 호출되지 않는다")
        void should_throwDevNoteNotFound_when_devNoteDoesNotExistOrOwnedByOther() {
            // given: mock DevNote 로 update 호출 자체 여부를 검증
            DevNote devNote = mock(DevNote.class);
            // 실제 흐름: findByIdAndAuthor_Id 가 empty → 예외, mock 노트는 사용되지 않음
            given(devNoteRepository.findByIdAndAuthor_Id(NOTE_ID, AUTHOR_ID))
                    .willReturn(Optional.empty());

            DevNoteUpdateRequest req = new DevNoteUpdateRequest("t", "c");

            // when / then
            assertThatThrownBy(() -> devNoteService.update(NOTE_ID, AUTHOR_ID, req))
                    .isInstanceOf(DevNoteNotFoundException.class);

            verify(devNote, never()).update(any(), any());
            verify(devNoteRepository, never()).saveAndFlush(any());
        }
    }

    // =====================================================================
    // 5. delete
    // =====================================================================

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("정상 삭제 — 본인 소유 노트면 devNoteRepository.delete(devNote) 를 호출한다")
        void should_deleteDevNote_when_devNoteExistsForAuthor() {
            // given
            User author = buildAuthor(AUTHOR_ID);
            DevNote devNote = buildPersistedDevNote(NOTE_ID, author, "title", "content");
            given(devNoteRepository.findByIdAndAuthor_Id(NOTE_ID, AUTHOR_ID))
                    .willReturn(Optional.of(devNote));

            // when
            devNoteService.delete(NOTE_ID, AUTHOR_ID);

            // then
            verify(devNoteRepository).delete(devNote);
        }

        @Test
        @DisplayName("미존재 또는 타인 소유면 DevNoteNotFoundException 을 던지고 delete 는 호출되지 않는다")
        void should_throwDevNoteNotFound_when_devNoteDoesNotExistOrOwnedByOther() {
            // given
            given(devNoteRepository.findByIdAndAuthor_Id(NOTE_ID, AUTHOR_ID))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> devNoteService.delete(NOTE_ID, AUTHOR_ID))
                    .isInstanceOf(DevNoteNotFoundException.class);

            verify(devNoteRepository, never()).delete(any(DevNote.class));
        }
    }
}
