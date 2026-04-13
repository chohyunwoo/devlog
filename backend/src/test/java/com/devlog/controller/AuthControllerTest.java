package com.devlog.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devlog.common.exception.DuplicateEmailException;
import com.devlog.common.exception.DuplicateNicknameException;
import com.devlog.common.exception.DuplicateUserException;
import com.devlog.common.exception.GlobalExceptionHandler;
import com.devlog.controller.dto.UserSignupRequest;
import com.devlog.domain.User;
import com.devlog.security.JwtAuthenticationFilter;
import com.devlog.security.SecurityConfig;
import com.devlog.service.UserService;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * AuthController 슬라이스 테스트.
 *
 * 설계 메모
 * - SecurityConfig 는 JwtProvider(+JwtProperties) 빈을 요구한다. 슬라이스 테스트에서는
 *   @WebMvcTest 의 excludeFilters 로 SecurityConfig/JwtAuthenticationFilter 를 스캔에서 제외하고,
 *   @AutoConfigureMockMvc(addFilters = false) 로 Spring Security 필터 체인 자체를 우회한다.
 *   /api/auth/signup 은 비인증 엔드포인트이므로 인증 컨텍스트가 없어도 무방하다.
 * - GlobalExceptionHandler 는 @Import 로 명시적으로 로드한다.
 * - UserService 는 @MockitoBean 으로 스텁 (Spring Boot 3.4+/4.x, @MockBean 대체).
 */
@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController 슬라이스 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private static final String SIGNUP_URL = "/api/auth/signup";

    private static final String VALID_EMAIL = "user@devlog.com";
    private static final String VALID_PASSWORD = "password123";
    private static final String VALID_NICKNAME = "devlog-user";

    private static UserSignupRequest validRequest() {
        return new UserSignupRequest(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME);
    }

    /**
     * 테스트용 User 인스턴스를 만든다.
     * - User 의 생성자/팩토리는 PasswordEncoder 를 요구하므로 실제 BCryptPasswordEncoder 를 사용한다.
     * - id / createdAt 은 JPA 가 관리하므로 reflection 으로 세팅한다.
     */
    private static User buildPersistedUser(Long id, String email, String nickname, LocalDateTime createdAt) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = User.create(email, VALID_PASSWORD, nickname, encoder);
        setField(user, "id", id);
        setField(user, "createdAt", createdAt);
        return user;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = User.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("failed to set field: " + fieldName, e);
        }
    }

    // ---------------------------------------------------------------------
    // 1. 정상 가입
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("정상 가입")
    class HappyPath {

        @Test
        @DisplayName("유효한 요청이면 201 Created 와 id/email/nickname/createdAt 을 반환한다")
        void should_return201WithUserResponse_when_requestIsValid() throws Exception {
            // given
            LocalDateTime createdAt = LocalDateTime.of(2026, 4, 13, 10, 0, 0);
            User persisted = buildPersistedUser(42L, VALID_EMAIL, VALID_NICKNAME, createdAt);
            given(userService.signup(any(UserSignupRequest.class))).willReturn(persisted);

            // when
            String body = objectMapper.writeValueAsString(validRequest());

            // then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(42))
                    .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                    .andExpect(jsonPath("$.nickname").value(VALID_NICKNAME))
                    .andExpect(jsonPath("$.createdAt").exists());

            verify(userService).signup(any(UserSignupRequest.class));
        }

        @Test
        @DisplayName("성공 응답 JSON 에는 password 키가 존재하지 않는다 (PII 회귀 방어)")
        void should_notLeakPasswordField_inResponseBody() throws Exception {
            // given
            User persisted = buildPersistedUser(
                    1L, VALID_EMAIL, VALID_NICKNAME, LocalDateTime.now());
            given(userService.signup(any(UserSignupRequest.class))).willReturn(persisted);
            String body = objectMapper.writeValueAsString(validRequest());

            // when / then: 응답 최상위 키 집합에 "password" 가 없어야 한다
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.*", not(hasItem(VALID_PASSWORD))));
        }
    }

    // ---------------------------------------------------------------------
    // 2. Bean Validation 실패 (400 INVALID_REQUEST)
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("요청 본문 검증 실패")
    class ValidationFailure {

        @Test
        @DisplayName("이메일 형식이 아니면 400 INVALID_REQUEST 를 반환하고 fieldErrors 에 email 이 포함된다")
        void should_return400_when_emailIsMalformed() throws Exception {
            // given
            UserSignupRequest req = new UserSignupRequest("not-an-email", VALID_PASSWORD, VALID_NICKNAME);
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 400 INVALID_REQUEST 를 반환하고 fieldErrors 에 password 가 포함된다")
        void should_return400_when_passwordTooShort() throws Exception {
            // given
            UserSignupRequest req = new UserSignupRequest(VALID_EMAIL, "short", VALID_NICKNAME);
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
        }

        @Test
        @DisplayName("닉네임이 빈 문자열이면 400 INVALID_REQUEST 를 반환하고 fieldErrors 에 nickname 이 포함된다")
        void should_return400_when_nicknameIsBlank() throws Exception {
            // given
            UserSignupRequest req = new UserSignupRequest(VALID_EMAIL, VALID_PASSWORD, "");
            String body = objectMapper.writeValueAsString(req);

            // when / then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'nickname')]").exists());
        }
    }

    // ---------------------------------------------------------------------
    // 3. 중복 예외 -> 409 Conflict
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("중복 충돌 응답")
    class DuplicateConflicts {

        @Test
        @DisplayName("이메일 중복이면 409 Conflict 와 code=DUPLICATE_EMAIL 를 반환한다")
        void should_return409_when_serviceThrowsDuplicateEmail() throws Exception {
            // given
            given(userService.signup(any(UserSignupRequest.class)))
                    .willThrow(new DuplicateEmailException());
            String body = objectMapper.writeValueAsString(validRequest());

            // when / then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                    .andExpect(jsonPath("$.message").value("이미 등록된 이메일입니다."))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors").isEmpty());
        }

        @Test
        @DisplayName("닉네임 중복이면 409 Conflict 와 code=DUPLICATE_NICKNAME 를 반환한다")
        void should_return409_when_serviceThrowsDuplicateNickname() throws Exception {
            // given
            given(userService.signup(any(UserSignupRequest.class)))
                    .willThrow(new DuplicateNicknameException());
            String body = objectMapper.writeValueAsString(validRequest());

            // when / then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE_NICKNAME"));
        }

        @Test
        @DisplayName("TOCTOU race fallback 이면 409 Conflict 와 code=DUPLICATE_USER 를 반환한다")
        void should_return409_when_serviceThrowsDuplicateUser() throws Exception {
            // given
            given(userService.signup(any(UserSignupRequest.class)))
                    .willThrow(new DuplicateUserException());
            String body = objectMapper.writeValueAsString(validRequest());

            // when / then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE_USER"));
        }
    }

    // ---------------------------------------------------------------------
    // 4. 잘못된 JSON 본문 -> 400 MALFORMED_JSON
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("잘못된 요청 본문")
    class MalformedBody {

        @Test
        @DisplayName("JSON 문법 오류가 있으면 400 MALFORMED_JSON 을 반환한다")
        void should_return400_when_jsonIsMalformed() throws Exception {
            // given: 닫는 중괄호가 없는 깨진 JSON
            String malformed = "{\"email\":\"user@devlog.com\",\"password\":\"password123\"";

            // when / then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformed))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                    .andExpect(jsonPath("$.message").value("요청 본문을 해석할 수 없습니다."));
        }
    }

    // ---------------------------------------------------------------------
    // 5. 응답 매핑 세부 검증
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("응답 매핑")
    class ResponseMapping {

        @Test
        @DisplayName("응답 본문의 키 집합은 id/email/nickname/createdAt 네 개만 존재한다")
        void should_exposeExactlyFourFields_inResponseBody() throws Exception {
            // given
            User persisted = buildPersistedUser(
                    7L, VALID_EMAIL, VALID_NICKNAME,
                    LocalDateTime.of(2026, 4, 13, 12, 34, 56));
            given(userService.signup(any(UserSignupRequest.class))).willReturn(persisted);
            String body = objectMapper.writeValueAsString(validRequest());

            // when
            String responseBody = mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            // then: LinkedHashMap 으로 파싱해 최상위 키 집합을 직접 검사
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(responseBody, LinkedHashMap.class);
            org.assertj.core.api.Assertions.assertThat(parsed.keySet())
                    .containsExactlyInAnyOrder("id", "email", "nickname", "createdAt");
        }
    }
}
