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

import com.devlog.common.exception.GlobalExceptionHandler;
import com.devlog.common.exception.PostAccessDeniedException;
import com.devlog.common.exception.PostNotFoundException;
import com.devlog.controller.dto.PostCreateRequest;
import com.devlog.controller.dto.PostUpdateRequest;
import com.devlog.domain.Post;
import com.devlog.domain.User;
import com.devlog.security.AuthenticatedUser;
import com.devlog.security.JwtAuthenticationFilter;
import com.devlog.security.SecurityConfig;
import com.devlog.service.PostService;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * PostController 슬라이스 테스트.
 *
 * 설계 메모
 * - SecurityConfig / JwtAuthenticationFilter 는 excludeFilters 로 스캔에서 제외하고,
 *   @AutoConfigureMockMvc(addFilters = false) 로 Spring Security 필터 체인을 우회한다.
 * - @AuthenticationPrincipal AuthenticatedUser 주입은 AuthenticationPrincipalArgumentResolver 가
 *   SecurityContextHolder 를 조회하는 방식이므로, 필터 체인이 꺼진 상태에서는 각 테스트 전에
 *   SecurityContextHolder 에 Authentication 을 직접 세팅하고 @AfterEach 에서 clear 한다.
 *   (SecurityMockMvcRequestPostProcessors.authentication(...) 은 SecurityContextHolderFilter 를
 *    요구하므로 addFilters=false 환경에서는 동작하지 않는다.)
 * - GlobalExceptionHandler 는 @Import 로 명시적으로 로드한다.
 * - PostService 는 @MockitoBean 으로 스텁한다.
 */
@WebMvcTest(
        controllers = PostController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PostController 슬라이스 테스트")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    private static final String POSTS_URL = "/api/posts";

    private static final Long AUTHOR_ID = 42L;
    private static final String AUTHOR_EMAIL = "author@devlog.com";
    private static final String AUTHOR_NICKNAME = "author";
    private static final Long POST_ID = 7L;
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 4, 13, 10, 0, 0);

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ---------- 픽스처 헬퍼 ---------- //

    private AuthenticatedUser principal() {
        return new AuthenticatedUser(AUTHOR_ID, AUTHOR_EMAIL);
    }

    /**
     * 인증이 필요한 엔드포인트용 기본 principal 세팅.
     * 각 테스트 메서드 시작 시 호출되어 SecurityContextHolder 에 Authentication 을 주입한다.
     * 인증이 필요 없는(또는 principal 이 null 이어야 하는) 케이스에서는 {@link #clearAuth()} 로 지운다.
     */
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

    private Post buildPersistedPost(Long postId, User author, String title, String contentText, Set<String> tags) {
        Post post = Post.create(title, contentText, author, tags);
        setField(Post.class, post, "id", postId);
        setField(Post.class, post, "createdAt", FIXED_TIME);
        setField(Post.class, post, "updatedAt", FIXED_TIME);
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
    // POST /api/posts — 생성
    // =====================================================================

    @Nested
    @DisplayName("POST /api/posts")
    class CreatePost {

        @Test
        @DisplayName("인증 + 유효 요청이면 201 Created 와 PostResponse 를 반환한다")
        void should_return201WithPostResponse_when_requestIsValid() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Set<String> tags = new LinkedHashSet<>(List.of("java", "spring"));
            Post created = buildPersistedPost(POST_ID, author, "title", "content", tags);
            given(postService.create(eq(AUTHOR_ID), any(PostCreateRequest.class))).willReturn(created);

            PostCreateRequest req = new PostCreateRequest("title", "content", tags);
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(post(POSTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(POST_ID))
                    .andExpect(jsonPath("$.title").value("title"))
                    .andExpect(jsonPath("$.content").value("content"))
                    .andExpect(jsonPath("$.author.id").value(AUTHOR_ID))
                    .andExpect(jsonPath("$.author.nickname").value(AUTHOR_NICKNAME))
                    .andExpect(jsonPath("$.tags").isArray())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());

            verify(postService).create(eq(AUTHOR_ID), any(PostCreateRequest.class));
        }

        @Test
        @DisplayName("title 이 빈 문자열이면 400 INVALID_REQUEST 를 반환하고 fieldErrors 에 title 이 포함된다")
        void should_return400_when_titleIsBlank() throws Exception {
            // given
            PostCreateRequest req = new PostCreateRequest("", "content", Set.of());
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(post(POSTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").exists());
        }

        @Test
        @DisplayName("title 이 201자(상한 초과)면 400 INVALID_REQUEST 를 반환한다")
        void should_return400_when_titleExceedsMaxLength() throws Exception {
            // given: Post.MAX_TITLE_LENGTH == 200, 따라서 201자는 @Size 위반
            String tooLong = "a".repeat(201);
            PostCreateRequest req = new PostCreateRequest(tooLong, "content", Set.of());
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(post(POSTS_URL)
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
            PostCreateRequest req = new PostCreateRequest("title", "", Set.of());
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(post(POSTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'content')]").exists());
        }

        @Test
        @DisplayName("tags 원소 중 빈 문자열이 있으면 400 INVALID_REQUEST 를 반환한다 (element level @NotBlank)")
        void should_return400_when_tagElementIsBlank() throws Exception {
            // given: tags 에 빈 문자열 포함
            Set<String> invalidTags = new LinkedHashSet<>(List.of("java", ""));
            PostCreateRequest req = new PostCreateRequest("title", "content", invalidTags);
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(post(POSTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
    }

    // =====================================================================
    // GET /api/posts — 목록
    // =====================================================================

    @Nested
    @DisplayName("GET /api/posts")
    class ListPosts {

        @Test
        @DisplayName("기본 목록 조회 — 200 OK 와 PageResponse 스키마를 반환한다")
        void should_return200WithPageResponse_when_noFilterIsProvided() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, "t1", "c1", Set.of("java"));
            Pageable pageable = PageRequest.of(0, 20);
            Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
            given(postService.findAll(any(Pageable.class))).willReturn(page);

            // when / then
            mockMvc.perform(get(POSTS_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(POST_ID))
                    .andExpect(jsonPath("$.content[0].title").value("t1"))
                    .andExpect(jsonPath("$.content[0].author.id").value(AUTHOR_ID))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true));

            verify(postService).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("authorId 필터가 주어지면 findByAuthor 를 호출한다")
        void should_callFindByAuthor_when_authorIdQueryIsProvided() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, "t1", "c1", Set.of());
            Pageable pageable = PageRequest.of(0, 20);
            Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
            given(postService.findByAuthor(eq(AUTHOR_ID), any(Pageable.class))).willReturn(page);

            // when / then
            mockMvc.perform(get(POSTS_URL).param("authorId", String.valueOf(AUTHOR_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(POST_ID));

            verify(postService).findByAuthor(eq(AUTHOR_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("tag 필터가 주어지면 findByTag 를 호출한다")
        void should_callFindByTag_when_tagQueryIsProvided() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, "t1", "c1", Set.of("java"));
            Pageable pageable = PageRequest.of(0, 20);
            Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
            given(postService.findByTag(eq("java"), any(Pageable.class))).willReturn(page);

            // when / then
            mockMvc.perform(get(POSTS_URL).param("tag", "java"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(POST_ID));

            verify(postService).findByTag(eq("java"), any(Pageable.class));
        }

        @Test
        @DisplayName("authorId 와 tag 를 동시 지정하면 400 INVALID_REQUEST 를 반환한다")
        void should_return400_when_authorIdAndTagAreBothProvided() throws Exception {
            // when / then
            mockMvc.perform(get(POSTS_URL)
                            .param("authorId", String.valueOf(AUTHOR_ID))
                            .param("tag", "java"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message")
                            .value("authorId와 tag 는 동시에 지정할 수 없습니다."));
        }
    }

    // =====================================================================
    // GET /api/posts/{postId} — 상세
    // =====================================================================

    @Nested
    @DisplayName("GET /api/posts/{postId}")
    class GetDetail {

        @Test
        @DisplayName("존재하는 postId 면 200 OK 와 PostResponse 를 반환한다")
        void should_return200WithPostResponse_when_postExists() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, "title", "content", Set.of("java"));
            given(postService.findDetail(POST_ID)).willReturn(post);

            // when / then
            mockMvc.perform(get(POSTS_URL + "/" + POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(POST_ID))
                    .andExpect(jsonPath("$.title").value("title"))
                    .andExpect(jsonPath("$.content").value("content"))
                    .andExpect(jsonPath("$.author.id").value(AUTHOR_ID))
                    .andExpect(jsonPath("$.author.nickname").value(AUTHOR_NICKNAME));
        }

        @Test
        @DisplayName("서비스가 PostNotFoundException 을 던지면 404 POST_NOT_FOUND 를 반환한다")
        void should_return404_when_serviceThrowsPostNotFound() throws Exception {
            // given
            given(postService.findDetail(POST_ID)).willThrow(new PostNotFoundException());

            // when / then
            mockMvc.perform(get(POSTS_URL + "/" + POST_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("요청한 글을 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("postId 가 숫자가 아니면 400 INVALID_PARAMETER 를 반환한다 (MethodArgumentTypeMismatch)")
        void should_return400_when_postIdIsNotNumeric() throws Exception {
            // when / then
            mockMvc.perform(get(POSTS_URL + "/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
    }

    // =====================================================================
    // PUT /api/posts/{postId} — 수정
    // =====================================================================

    @Nested
    @DisplayName("PUT /api/posts/{postId}")
    class UpdatePost {

        @Test
        @DisplayName("인증 + 유효 요청 + 본인 글이면 200 OK 와 PostResponse 를 반환한다")
        void should_return200_when_updateSucceeds() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post updated = buildPersistedPost(POST_ID, author, "new title", "new content", Set.of("new"));
            given(postService.update(eq(POST_ID), eq(AUTHOR_ID), any(PostUpdateRequest.class)))
                    .willReturn(updated);

            PostUpdateRequest req = new PostUpdateRequest("new title", "new content", Set.of("new"));
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(put(POSTS_URL + "/" + POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(POST_ID))
                    .andExpect(jsonPath("$.title").value("new title"))
                    .andExpect(jsonPath("$.content").value("new content"));

            verify(postService).update(eq(POST_ID), eq(AUTHOR_ID), any(PostUpdateRequest.class));
        }

        @Test
        @DisplayName("본문 검증 실패(title 빈 문자열)면 400 INVALID_REQUEST 를 반환한다")
        void should_return400_when_updateBodyIsInvalid() throws Exception {
            // given
            PostUpdateRequest req = new PostUpdateRequest("", "content", Set.of());
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(put(POSTS_URL + "/" + POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").exists());
        }

        @Test
        @DisplayName("다른 사용자가 수정을 시도하면 (PostAccessDeniedException) 403 FORBIDDEN 을 반환한다")
        void should_return403_when_requesterIsNotAuthor() throws Exception {
            // given
            given(postService.update(anyLong(), anyLong(), any(PostUpdateRequest.class)))
                    .willThrow(new PostAccessDeniedException());

            PostUpdateRequest req = new PostUpdateRequest("t", "c", Set.of());
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(put(POSTS_URL + "/" + POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("없는 Post 를 수정 시도하면 (PostNotFoundException) 404 POST_NOT_FOUND 를 반환한다")
        void should_return404_when_postDoesNotExist() throws Exception {
            // given
            given(postService.update(anyLong(), anyLong(), any(PostUpdateRequest.class)))
                    .willThrow(new PostNotFoundException());

            PostUpdateRequest req = new PostUpdateRequest("t", "c", Set.of());
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(put(POSTS_URL + "/" + POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        }
    }

    // =====================================================================
    // DELETE /api/posts/{postId}
    // =====================================================================

    @Nested
    @DisplayName("DELETE /api/posts/{postId}")
    class DeletePost {

        @Test
        @DisplayName("본인 글 삭제 시 204 No Content 와 빈 본문을 반환한다")
        void should_return204_when_deleteSucceeds() throws Exception {
            // when / then
            mockMvc.perform(delete(POSTS_URL + "/" + POST_ID))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(postService).delete(POST_ID, AUTHOR_ID);
        }

        @Test
        @DisplayName("다른 사용자가 삭제를 시도하면 403 FORBIDDEN 을 반환한다")
        void should_return403_when_requesterIsNotAuthor() throws Exception {
            // given
            org.mockito.BDDMockito.willThrow(new PostAccessDeniedException())
                    .given(postService).delete(anyLong(), anyLong());

            // when / then
            mockMvc.perform(delete(POSTS_URL + "/" + POST_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("없는 Post 삭제 시도 시 404 POST_NOT_FOUND 를 반환한다")
        void should_return404_when_postDoesNotExist() throws Exception {
            // given
            org.mockito.BDDMockito.willThrow(new PostNotFoundException())
                    .given(postService).delete(anyLong(), anyLong());

            // when / then
            mockMvc.perform(delete(POSTS_URL + "/" + POST_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        }
    }

    // =====================================================================
    // 응답 스키마 락 (키 집합 고정 — PII/over-posting 회귀 방어)
    // =====================================================================

    @Nested
    @DisplayName("응답 스키마 락")
    class ResponseSchemaLock {

        @Test
        @DisplayName("PostResponse 는 정확히 id/title/content/author/tags/createdAt/updatedAt 7개 키만 존재한다")
        void should_exposeExactlySevenFields_inPostResponseBody() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, "title", "content", Set.of("java"));
            given(postService.findDetail(POST_ID)).willReturn(post);

            // when
            String responseBody = mockMvc.perform(get(POSTS_URL + "/" + POST_ID))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // then
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(responseBody, LinkedHashMap.class);
            org.assertj.core.api.Assertions.assertThat(parsed.keySet())
                    .containsExactlyInAnyOrder(
                            "id", "title", "content", "author", "tags", "createdAt", "updatedAt");
        }

        @Test
        @DisplayName("PageResponse 는 정확히 content/page/size/totalElements/totalPages/first/last 7개 키만 존재한다")
        void should_exposeExactlySevenFields_inPageResponseBody() throws Exception {
            // given
            User author = buildAuthor(AUTHOR_ID);
            Post post = buildPersistedPost(POST_ID, author, "title", "content", Set.of());
            Pageable pageable = PageRequest.of(0, 20);
            Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
            given(postService.findAll(any(Pageable.class))).willReturn(page);

            // when
            String responseBody = mockMvc.perform(get(POSTS_URL))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // then
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(responseBody, LinkedHashMap.class);
            org.assertj.core.api.Assertions.assertThat(parsed.keySet())
                    .containsExactlyInAnyOrder(
                            "content", "page", "size", "totalElements", "totalPages", "first", "last");
        }
    }
}
