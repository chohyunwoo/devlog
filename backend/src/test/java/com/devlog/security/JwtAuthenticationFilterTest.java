package com.devlog.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("JwtAuthenticationFilter 단위 테스트")
class JwtAuthenticationFilterTest {

    private static final String AUTH_HEADER = "Authorization";

    private JwtProvider jwtProvider;
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtProvider = mock(JwtProvider.class);
        filter = new JwtAuthenticationFilter(jwtProvider);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * MockFilterChain 은 doFilter 가 호출되면 내부적으로 request/response 를 보관한다.
     * 따라서 chain.getRequest() != null 이면 다음 필터로 체인이 넘어갔다는 뜻이다.
     */
    private boolean chainProceeded() {
        return chain.getRequest() != null;
    }

    // ---------------------------------------------------------------------
    // 1. Authorization 헤더 없음
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Authorization 헤더 처리")
    class AuthorizationHeaderHandling {

        @Test
        @DisplayName("헤더가 없으면 SecurityContext는 비어 있고 체인만 진행된다")
        void should_skipAuthentication_when_noAuthorizationHeader() throws Exception {
            // given: 어떤 Authorization 헤더도 없는 요청

            // when
            filter.doFilter(request, response, chain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(chainProceeded()).isTrue();
            verify(jwtProvider, never()).validateAccessToken(anyString());
            verify(jwtProvider, never()).getUserId(anyString());
            verify(jwtProvider, never()).getEmail(anyString());
        }

        @Test
        @DisplayName("Non-Bearer 스킴(Basic)은 무시되고 검증도 시도하지 않는다")
        void should_skipAuthentication_when_nonBearerScheme() throws Exception {
            // given
            request.addHeader(AUTH_HEADER, "Basic dXNlcjpwYXNz");

            // when
            filter.doFilter(request, response, chain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(chainProceeded()).isTrue();
            verify(jwtProvider, never()).validateAccessToken(anyString());
        }

        @Test
        @DisplayName("'Bearer ' 뒤가 빈 문자열이면 검증하지 않는다")
        void should_skipAuthentication_when_bearerWithEmptyToken() throws Exception {
            // given
            request.addHeader(AUTH_HEADER, "Bearer ");

            // when
            filter.doFilter(request, response, chain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(chainProceeded()).isTrue();
            verify(jwtProvider, never()).validateAccessToken(anyString());
        }

        @Test
        @DisplayName("'Bearer   ' (공백만)인 경우에도 검증하지 않는다")
        void should_skipAuthentication_when_bearerWithOnlyWhitespace() throws Exception {
            // given
            request.addHeader(AUTH_HEADER, "Bearer    ");

            // when
            filter.doFilter(request, response, chain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(chainProceeded()).isTrue();
            verify(jwtProvider, never()).validateAccessToken(anyString());
        }
    }

    // ---------------------------------------------------------------------
    // 2. 토큰 검증 결과별 동작
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("토큰 검증 결과 처리")
    class TokenValidationHandling {

        @Test
        @DisplayName("VALID 토큰이면 SecurityContext에 AuthenticatedUser principal이 설정된다")
        void should_setAuthentication_when_tokenIsValid() throws Exception {
            // given
            request.addHeader(AUTH_HEADER, "Bearer tkn");
            when(jwtProvider.validateAccessToken("tkn")).thenReturn(TokenValidationResult.VALID);
            when(jwtProvider.getUserId("tkn")).thenReturn(42L);
            when(jwtProvider.getEmail("tkn")).thenReturn("user@devlog.com");

            // when
            filter.doFilter(request, response, chain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
            assertThat(authentication.isAuthenticated()).isTrue();
            assertThat(authentication.getAuthorities()).isEqualTo(Collections.emptyList());
            assertThat(authentication.getCredentials()).isNull();
            assertThat(authentication.getPrincipal())
                    .isInstanceOf(AuthenticatedUser.class)
                    .isEqualTo(new AuthenticatedUser(42L, "user@devlog.com"));
            assertThat(chainProceeded()).isTrue();

            verify(jwtProvider).validateAccessToken("tkn");
            verify(jwtProvider).getUserId("tkn");
            verify(jwtProvider).getEmail("tkn");
        }

        @Test
        @DisplayName("EXPIRED 토큰이면 SecurityContext는 비어 있고 체인만 진행된다")
        void should_skipAuthentication_when_tokenExpired() throws Exception {
            // given
            request.addHeader(AUTH_HEADER, "Bearer expired.jwt.token");
            when(jwtProvider.validateAccessToken("expired.jwt.token"))
                    .thenReturn(TokenValidationResult.EXPIRED);

            // when
            filter.doFilter(request, response, chain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(chainProceeded()).isTrue();
            verify(jwtProvider, never()).getUserId(anyString());
            verify(jwtProvider, never()).getEmail(anyString());
        }

        @Test
        @DisplayName("INVALID 토큰이면 SecurityContext는 비어 있고 체인만 진행된다")
        void should_skipAuthentication_when_tokenInvalid() throws Exception {
            // given
            request.addHeader(AUTH_HEADER, "Bearer bogus.jwt.token");
            when(jwtProvider.validateAccessToken("bogus.jwt.token"))
                    .thenReturn(TokenValidationResult.INVALID);

            // when
            filter.doFilter(request, response, chain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(chainProceeded()).isTrue();
            verify(jwtProvider, never()).getUserId(anyString());
            verify(jwtProvider, never()).getEmail(anyString());
        }
    }

    // ---------------------------------------------------------------------
    // 3. Bearer prefix 대소문자 무시 + 공백 trim
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Bearer prefix는 대소문자를 구분하지 않고 토큰 주변 공백은 trim된다")
    void should_parseToken_caseInsensitiveBearer() throws Exception {
        // given: 소문자 bearer + 앞뒤 공백이 섞인 헤더
        request.addHeader(AUTH_HEADER, "bearer abc.def.ghi");
        when(jwtProvider.validateAccessToken("abc.def.ghi"))
                .thenReturn(TokenValidationResult.VALID);
        when(jwtProvider.getUserId("abc.def.ghi")).thenReturn(7L);
        when(jwtProvider.getEmail("abc.def.ghi")).thenReturn("lower@devlog.com");

        // when
        filter.doFilter(request, response, chain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthenticatedUser(7L, "lower@devlog.com"));
        assertThat(chainProceeded()).isTrue();

        // 공백이 제거되고 정확히 "abc.def.ghi" 로 호출되었는지 검증
        verify(jwtProvider).validateAccessToken("abc.def.ghi");
        verify(jwtProvider).getUserId("abc.def.ghi");
        verify(jwtProvider).getEmail("abc.def.ghi");
    }

    // ---------------------------------------------------------------------
    // 4. 진입 시 기존 SecurityContext 오염 제거 (H2 회귀 방어)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("필터 진입 시 기존 SecurityContext는 항상 초기화된다")
    void should_clearExistingContext_onEntry() throws Exception {
        // given: 이전 필터/스레드 재사용 등으로 인해 오염된 SecurityContext 가정
        Authentication stale = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(999L, "stale@devlog.com"),
                null,
                Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(stale);
        // 헤더 없는 요청 -> 인증 세팅 로직은 타지 않음

        // when
        filter.doFilter(request, response, chain);

        // then: 진입 시 clearContext() 가 호출되었으므로 이전 오염이 제거되어야 한다
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chainProceeded()).isTrue();
        verify(jwtProvider, never()).validateAccessToken(anyString());
    }

    // ---------------------------------------------------------------------
    // 5. 검증 이후 예외가 필터 밖으로 새지 않아야 함 (H1 회귀 방어)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("VALID 이후 getUserId가 런타임 예외를 던져도 필터는 체인을 계속 진행한다")
    void should_swallowException_when_providerThrowsAfterValidation() throws Exception {
        // given
        request.addHeader(AUTH_HEADER, "Bearer tkn");
        when(jwtProvider.validateAccessToken("tkn")).thenReturn(TokenValidationResult.VALID);
        when(jwtProvider.getUserId("tkn"))
                .thenThrow(new IllegalStateException("JWT subject (userId) is missing"));

        // when
        filter.doFilter(request, response, chain);

        // then: catch 블록에서 clearContext() 가 호출되고 예외는 밖으로 새지 않아야 한다
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chainProceeded()).isTrue();
        verify(jwtProvider).validateAccessToken("tkn");
        verify(jwtProvider).getUserId("tkn");
    }
}
