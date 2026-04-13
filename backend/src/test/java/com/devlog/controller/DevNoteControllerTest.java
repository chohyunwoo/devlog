package com.devlog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devlog.common.exception.DevNoteNotFoundException;
import com.devlog.common.exception.GlobalExceptionHandler;
import com.devlog.controller.dto.DevNoteCreateRequest;
import com.devlog.controller.dto.DevNoteUpdateRequest;
import com.devlog.domain.DevNote;
import com.devlog.domain.User;
import com.devlog.security.AuthenticatedUser;
import com.devlog.security.JwtAuthenticationFilter;
import com.devlog.security.SecurityConfig;
import com.devlog.service.DevNoteService;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * DevNoteController 슬라이스 테스트.
 *
 * 설계 메모
 * - PostControllerTest 와 동일한 패턴: SecurityConfig / JwtAuthenticationFilter 를 스캔에서 제외하고
 *   addFilters=false 로 필터 체인을 우회한다. principal 은 SecurityContextHolder 에 직접 주입한다.
 * - DevNote 는 비공개 엔티티이므로 PostController 와 달리 다음 차이가 있다:
 *   1) 모든 엔드포인트가 인증 필수(공개 경로 없음)
 *   2) 타인 소유 노트 접근 시 403 이 아닌 404(DEV_NOTE_NOT_FOUND) — 소유권 검증을 리포지토리가 책임
 *   3) list 에 authorId/tag 같은 쿼리 파라미터 없음 — 항상 본인 것만
 *   4) 응답 스키마에 author 필드 부재 (DevNoteResponse 5키, DevNoteSummaryResponse 4키)
 */
@WebMvcTest(
        controllers = DevNoteController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DevNoteController 슬라이스 테스트")
class DevNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DevNoteService devNoteService;

    private static final String DEV_NOTES_URL = "/api/dev-notes";

    private static final Long AUTHOR_ID = 42L;
    private static final String AUTHOR_EMAIL = "author@devlog.com";
    private static final String AUTHOR_NICKNAME = "author";
    private static final Long NOTE_ID = 7L;
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 4, 13, 10, 0, 0);

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ---------- 픽스처 헬퍼 ---------- //

    private AuthenticatedUser principal() {
        return new AuthenticatedUser(AUTHOR_ID, AUTHOR_EMAIL);
    }

    @BeforeEach
    void setUpSecurityContext() {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(principal(), null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private User buildAuthor(Long id) {
        User user = User.create(AUTHOR_EMAIL, "password1234", AUTHOR_NICKNAME, passwordEncoder);
        setField(User.class, user, "id", id);
        setField(User.class, user, "createdAt", FIXED_TIME);
        return user;
    }

    private DevNote buildPersistedDevNote(Long noteId, User author, String title, String contentText) {
        DevNote devNote = DevNote.create(title, contentText, author);
        setField(DevNote.class, devNote, "id", noteId);
        setField(DevNote.class, devNote, "createdAt", FIXED_TIME);
        setField(DevNote.class, devNote, "updatedAt", FIXED_TIME);
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
    // POST /api/dev-notes — 생성
    // =====================================================================

    @Nested
    @DisplayName("POST /api/dev-notes")
    class CreateDevNote {

        @Test
        @DisplayName("인증 + 유효 요청이면 201 Created 와 DevNoteResponse 를 반환한다")
        void should_return201WithDevNoteResponse_when_requestIsValid() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            DevNote created = buildPersistedDevNote(NOTE_ID, author, "title", "content");
            given(devNoteService.create(eq(AUTHOR_ID), any(DevNoteCreateRequest.class)))
                    .willReturn(created);

            DevNoteCreateRequest req = new DevNoteCreateRequest("title", "content");
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(post(DEV_NOTES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(NOTE_ID))
                    .andExpect(jsonPath("$.title").value("title"))
                    .andExpect(jsonPath("$.content").value("content"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());

            verify(devNoteService).create(eq(AUTHOR_ID), any(DevNoteCreateRequest.class));
        }

        @Test
        @DisplayName("title 이 빈 문자열이면 400 INVALID_REQUEST 를 반환하고 fieldErrors 에 title 이 포함된다")
        void should_return400_when_titleIsBlank() throws Exception {
            // given
            DevNoteCreateRequest req = new DevNoteCreateRequest("", "content");
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(post(DEV_NOTES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").exists());
        }

        @Test
        @DisplayName("title 이 201자(상한 초과)면 400 INVALID_REQUEST 를 반환한다")
        void should_return400_when_titleExceedsMaxLength() throws Exception {
            // given: DevNote.MAX_TITLE_LENGTH == 200, 따라서 201자는 @Size 위반
            String tooLong = "a".repeat(201);
            DevNoteCreateRequest req = new DevNoteCreateRequest(tooLong, "content");
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(post(DEV_NOTES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").exists());
        }

        @Test
        @DisplayName("content 가 빈 문자열이면 400 INVALID_REQUEST 를 반환한다")
        void should_return400_when_contentIsBlank() throws Exception {
            // given
            DevNoteCreateRequest req = new DevNoteCreateRequest("title", "");
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(post(DEV_NOTES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'content')]").exists());
        }

        @Test
        @DisplayName("JSON 본문이 잘못된 경우 400 MALFORMED_JSON 을 반환한다")
        void should_return400_when_requestBodyIsMalformed() throws Exception {
            // given
            String malformedBody = "{ this is not json";

            // when / then
            mockMvc.perform(post(DEV_NOTES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
        }
    }

    // =====================================================================
    // GET /api/dev-notes — 목록
    // =====================================================================

    @Nested
    @DisplayName("GET /api/dev-notes")
    class ListDevNotes {

        @Test
        @DisplayName("기본 목록 조회 — 200 OK 와 PageResponse 스키마를 반환한다 (본인 것만)")
        void should_return200WithPageResponse_when_listIsRequested() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            DevNote devNote = buildPersistedDevNote(NOTE_ID, author, "t1", "c1");
            Pageable pageable = PageRequest.of(0, 20);
            Page<DevNote> page = new PageImpl<>(List.of(devNote), pageable, 1);
            given(devNoteService.findByAuthor(eq(AUTHOR_ID), any(Pageable.class))).willReturn(page);

            // when / then
            mockMvc.perform(get(DEV_NOTES_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(NOTE_ID))
                    .andExpect(jsonPath("$.content[0].title").value("t1"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true));

            verify(devNoteService).findByAuthor(eq(AUTHOR_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("결과가 비어있으면 200 OK 와 빈 content 배열을 반환한다")
        void should_return200WithEmptyContent_when_noDevNotesExist() throws Exception {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<DevNote> page = new PageImpl<>(List.of(), pageable, 0);
            given(devNoteService.findByAuthor(eq(AUTHOR_ID), any(Pageable.class))).willReturn(page);

            // when / then
            mockMvc.perform(get(DEV_NOTES_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0));
        }
    }

    // =====================================================================
    // GET /api/dev-notes/{noteId} — 상세
    // =====================================================================

    @Nested
    @DisplayName("GET /api/dev-notes/{noteId}")
    class GetDetail {

        @Test
        @DisplayName("존재하는 noteId(본인 소유)면 200 OK 와 DevNoteResponse 를 반환한다")
        void should_return200WithDevNoteResponse_when_devNoteExists() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            DevNote devNote = buildPersistedDevNote(NOTE_ID, author, "title", "content");
            given(devNoteService.findDetail(NOTE_ID, AUTHOR_ID)).willReturn(devNote);

            // when / then
            mockMvc.perform(get(DEV_NOTES_URL + "/" + NOTE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(NOTE_ID))
                    .andExpect(jsonPath("$.title").value("title"))
                    .andExpect(jsonPath("$.content").value("content"));
        }

        @Test
        @DisplayName("미존재 또는 타인 소유면 (DevNoteNotFoundException) 404 DEV_NOTE_NOT_FOUND 를 반환한다 (403 분기 없음)")
        void should_return404_when_serviceThrowsDevNoteNotFound() throws Exception {
            // given: 비공개 노트라 "타인 소유"와 "미존재"가 동일 분기로 합쳐진다
            given(devNoteService.findDetail(NOTE_ID, AUTHOR_ID))
                    .willThrow(new DevNoteNotFoundException());

            // when / then
            mockMvc.perform(get(DEV_NOTES_URL + "/" + NOTE_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("DEV_NOTE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("요청한 DevNote를 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("noteId 가 숫자가 아니면 400 INVALID_PARAMETER 를 반환한다 (MethodArgumentTypeMismatch)")
        void should_return400_when_noteIdIsNotNumeric() throws Exception {
            // when / then
            mockMvc.perform(get(DEV_NOTES_URL + "/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
    }

    // =====================================================================
    // PUT /api/dev-notes/{noteId} — 수정
    // =====================================================================

    @Nested
    @DisplayName("PUT /api/dev-notes/{noteId}")
    class UpdateDevNote {

        @Test
        @DisplayName("인증 + 유효 요청 + 본인 노트면 200 OK 와 DevNoteResponse 를 반환한다")
        void should_return200_when_updateSucceeds() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            DevNote updated = buildPersistedDevNote(NOTE_ID, author, "new title", "new content");
            given(devNoteService.update(eq(NOTE_ID), eq(AUTHOR_ID), any(DevNoteUpdateRequest.class)))
                    .willReturn(updated);

            DevNoteUpdateRequest req = new DevNoteUpdateRequest("new title", "new content");
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(put(DEV_NOTES_URL + "/" + NOTE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(NOTE_ID))
                    .andExpect(jsonPath("$.title").value("new title"))
                    .andExpect(jsonPath("$.content").value("new content"));

            verify(devNoteService).update(eq(NOTE_ID), eq(AUTHOR_ID), any(DevNoteUpdateRequest.class));
        }

        @Test
        @DisplayName("본문 검증 실패(title 빈 문자열)면 400 INVALID_REQUEST 를 반환한다")
        void should_return400_when_updateBodyIsInvalid() throws Exception {
            // given
            DevNoteUpdateRequest req = new DevNoteUpdateRequest("", "content");
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(put(DEV_NOTES_URL + "/" + NOTE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").exists());
        }

        @Test
        @DisplayName("미존재 또는 타인 소유면 (DevNoteNotFoundException) 404 DEV_NOTE_NOT_FOUND 를 반환한다 (403 분기 없음)")
        void should_return404_when_devNoteDoesNotExistOrOwnedByOther() throws Exception {
            // given
            given(devNoteService.update(anyLong(), anyLong(), any(DevNoteUpdateRequest.class)))
                    .willThrow(new DevNoteNotFoundException());

            DevNoteUpdateRequest req = new DevNoteUpdateRequest("t", "c");
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(put(DEV_NOTES_URL + "/" + NOTE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("DEV_NOTE_NOT_FOUND"));
        }
    }

    // =====================================================================
    // DELETE /api/dev-notes/{noteId}
    // =====================================================================

    @Nested
    @DisplayName("DELETE /api/dev-notes/{noteId}")
    class DeleteDevNote {

        @Test
        @DisplayName("본인 노트 삭제 시 204 No Content 와 빈 본문을 반환한다")
        void should_return204_when_deleteSucceeds() throws Exception {
            // when / then
            mockMvc.perform(delete(DEV_NOTES_URL + "/" + NOTE_ID))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(devNoteService).delete(NOTE_ID, AUTHOR_ID);
        }

        @Test
        @DisplayName("미존재 또는 타인 소유 노트 삭제 시 404 DEV_NOTE_NOT_FOUND 를 반환한다 (403 분기 없음)")
        void should_return404_when_devNoteDoesNotExistOrOwnedByOther() throws Exception {
            // given
            org.mockito.BDDMockito.willThrow(new DevNoteNotFoundException())
                    .given(devNoteService).delete(anyLong(), anyLong());

            // when / then
            mockMvc.perform(delete(DEV_NOTES_URL + "/" + NOTE_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("DEV_NOTE_NOT_FOUND"));
        }
    }

    // =====================================================================
    // 응답 스키마 락 (키 집합 고정 — author 필드 부재 회귀 방어)
    // =====================================================================

    @Nested
    @DisplayName("응답 스키마 락")
    class ResponseSchemaLock {

        @Test
        @DisplayName("DevNoteResponse 는 정확히 id/title/content/createdAt/updatedAt 5개 키만 존재한다 (author 필드 없음)")
        void should_exposeExactlyFiveFields_inDevNoteResponseBody() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            DevNote devNote = buildPersistedDevNote(NOTE_ID, author, "title", "content");
            given(devNoteService.findDetail(NOTE_ID, AUTHOR_ID)).willReturn(devNote);

            // when
            String responseBody = mockMvc.perform(get(DEV_NOTES_URL + "/" + NOTE_ID))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // then
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(responseBody, LinkedHashMap.class);
            org.assertj.core.api.Assertions.assertThat(parsed.keySet())
                    .containsExactlyInAnyOrder(
                            "id", "title", "content", "createdAt", "updatedAt");
        }

        @Test
        @DisplayName("DevNoteSummaryResponse 는 정확히 id/title/createdAt/updatedAt 4개 키만 존재한다 (content/author 필드 없음)")
        void should_exposeExactlyFourFields_inDevNoteSummaryResponseBody() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            DevNote devNote = buildPersistedDevNote(NOTE_ID, author, "title", "content");
            Pageable pageable = PageRequest.of(0, 20);
            Page<DevNote> page = new PageImpl<>(List.of(devNote), pageable, 1);
            given(devNoteService.findByAuthor(eq(AUTHOR_ID), any(Pageable.class))).willReturn(page);

            // when
            String responseBody = mockMvc.perform(get(DEV_NOTES_URL))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // then
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(responseBody, LinkedHashMap.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) parsed.get("content");
            org.assertj.core.api.Assertions.assertThat(contentList).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(contentList.get(0).keySet())
                    .containsExactlyInAnyOrder("id", "title", "createdAt", "updatedAt");
        }
    }
}
