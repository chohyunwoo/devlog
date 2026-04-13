package com.devlog.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JwtProvider 단위 테스트")
class JwtProviderTest {

    // UTF-8 기준 32바이트 이상인 기본 시크릿 (영문 1자 = 1바이트)
    private static final String SECRET_32 = "0123456789abcdef0123456789abcdef"; // 정확히 32바이트
    private static final String SECRET_64 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"; // 64바이트
    private static final String OTHER_SECRET_64 =
            "FEDCBA9876543210FEDCBA9876543210FEDCBA9876543210FEDCBA9876543210"; // 다른 64바이트

    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(14);

    private static JwtProvider newProvider(String secret, Duration access, Duration refresh) {
        return new JwtProvider(new JwtProperties(secret, access, refresh));
    }

    private static JwtProvider defaultProvider() {
        return newProvider(SECRET_64, ACCESS_TTL, REFRESH_TTL);
    }

    // ---------------------------------------------------------------------
    // 생성자 / 시크릿 길이 검증
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("생성자 - 시크릿 길이 검증")
    class ConstructorSecretValidation {

        @Test
        @DisplayName("32바이트 이상 secret이면 정상 생성된다")
        void should_create_instance_when_secret_is_long_enough() {
            // given
            JwtProperties props = new JwtProperties(SECRET_64, ACCESS_TTL, REFRESH_TTL);

            // when / then
            assertThatCode(() -> new JwtProvider(props)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정확히 32바이트인 secret은 경계값으로 허용된다")
        void should_create_instance_when_secret_is_exactly_32_bytes() {
            // given
            assertThat(SECRET_32.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(32);
            JwtProperties props = new JwtProperties(SECRET_32, ACCESS_TTL, REFRESH_TTL);

            // when / then
            assertThatCode(() -> new JwtProvider(props)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("32바이트 미만 secret이면 IllegalStateException이 발생한다")
        void should_throw_when_secret_is_too_short() {
            // given
            JwtProperties props = new JwtProperties("short", ACCESS_TTL, REFRESH_TTL);

            // when / then
            assertThatThrownBy(() -> new JwtProvider(props))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("jwt.secret")
                    .hasMessageContaining("32");
        }

        @Test
        @DisplayName("31바이트(경계 바로 아래) secret도 거부된다")
        void should_throw_when_secret_is_31_bytes() {
            // given
            String secret31 = "0123456789abcdef0123456789abcde"; // 31 bytes
            assertThat(secret31.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(31);
            JwtProperties props = new JwtProperties(secret31, ACCESS_TTL, REFRESH_TTL);

            // when / then
            assertThatThrownBy(() -> new JwtProvider(props))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ---------------------------------------------------------------------
    // 발급 / 파싱 라운드트립
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("토큰 발급·파싱 라운드트립")
    class RoundTrip {

        @Test
        @DisplayName("access 토큰에서 userId와 email을 그대로 추출할 수 있다")
        void should_extract_userId_and_email_from_access_token() {
            // given
            JwtProvider provider = defaultProvider();
            Long userId = 1L;
            String email = "user@devlog.com";

            // when
            String token = provider.generateAccessToken(userId, email);

            // then
            assertThat(token).isNotBlank();
            assertThat(provider.getUserId(token)).isEqualTo(userId);
            assertThat(provider.getEmail(token)).isEqualTo(email);
        }

        @Test
        @DisplayName("refresh 토큰에서도 userId와 email을 추출할 수 있다")
        void should_extract_userId_and_email_from_refresh_token() {
            // given
            JwtProvider provider = defaultProvider();
            Long userId = 42L;
            String email = "alice@devlog.com";

            // when
            String token = provider.generateRefreshToken(userId, email);

            // then
            assertThat(provider.getUserId(token)).isEqualTo(userId);
            assertThat(provider.getEmail(token)).isEqualTo(email);
        }

        @Test
        @DisplayName("같은 입력으로 두 번 발급해도 jti가 달라 서로 다른 토큰이 나온다")
        void should_produce_distinct_tokens_due_to_jti() {
            // given
            JwtProvider provider = defaultProvider();

            // when
            String t1 = provider.generateAccessToken(1L, "user@devlog.com");
            String t2 = provider.generateAccessToken(1L, "user@devlog.com");

            // then
            assertThat(t1).isNotEqualTo(t2);
        }
    }

    // ---------------------------------------------------------------------
    // 타입 분리 검증 (access <-> refresh 회귀 방어)
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("토큰 타입 분리 검증")
    class TypeSeparation {

        @Test
        @DisplayName("access 토큰은 validateAccessToken에서 VALID이다")
        void should_return_valid_when_access_token_validated_as_access() {
            // given
            JwtProvider provider = defaultProvider();
            String token = provider.generateAccessToken(1L, "user@devlog.com");

            // when
            TokenValidationResult result = provider.validateAccessToken(token);

            // then
            assertThat(result).isEqualTo(TokenValidationResult.VALID);
        }

        @Test
        @DisplayName("access 토큰을 validateRefreshToken에 넣으면 INVALID이다")
        void should_return_invalid_when_access_token_validated_as_refresh() {
            // given
            JwtProvider provider = defaultProvider();
            String token = provider.generateAccessToken(1L, "user@devlog.com");

            // when
            TokenValidationResult result = provider.validateRefreshToken(token);

            // then
            assertThat(result).isEqualTo(TokenValidationResult.INVALID);
        }

        @Test
        @DisplayName("refresh 토큰은 validateRefreshToken에서 VALID이다")
        void should_return_valid_when_refresh_token_validated_as_refresh() {
            // given
            JwtProvider provider = defaultProvider();
            String token = provider.generateRefreshToken(1L, "user@devlog.com");

            // when
            TokenValidationResult result = provider.validateRefreshToken(token);

            // then
            assertThat(result).isEqualTo(TokenValidationResult.VALID);
        }

        @Test
        @DisplayName("refresh 토큰을 validateAccessToken에 넣으면 INVALID이다")
        void should_return_invalid_when_refresh_token_validated_as_access() {
            // given
            JwtProvider provider = defaultProvider();
            String token = provider.generateRefreshToken(1L, "user@devlog.com");

            // when
            TokenValidationResult result = provider.validateAccessToken(token);

            // then
            assertThat(result).isEqualTo(TokenValidationResult.INVALID);
        }
    }

    // ---------------------------------------------------------------------
    // 만료
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("만료 검증")
    class Expiration {

        @Test
        @DisplayName("과거 시점으로 만료된 access 토큰은 EXPIRED를 반환한다")
        void should_return_expired_when_access_token_is_past_expiration() {
            // given: clockSkew(30초)를 초과하는 음수 duration으로 즉시 만료 토큰 발급
            JwtProvider provider = newProvider(
                    SECRET_64,
                    Duration.ofSeconds(-60), // 60초 전에 이미 만료
                    REFRESH_TTL);
            String token = provider.generateAccessToken(1L, "user@devlog.com");

            // when
            TokenValidationResult result = provider.validateAccessToken(token);

            // then
            assertThat(result).isEqualTo(TokenValidationResult.EXPIRED);
        }

        @Test
        @DisplayName("과거 시점으로 만료된 refresh 토큰도 EXPIRED를 반환한다")
        void should_return_expired_when_refresh_token_is_past_expiration() {
            // given
            JwtProvider provider = newProvider(
                    SECRET_64,
                    ACCESS_TTL,
                    Duration.ofSeconds(-60));
            String token = provider.generateRefreshToken(1L, "user@devlog.com");

            // when
            TokenValidationResult result = provider.validateRefreshToken(token);

            // then
            assertThat(result).isEqualTo(TokenValidationResult.EXPIRED);
        }
    }

    // ---------------------------------------------------------------------
    // 서명 검증 (위조 방어)
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("서명 검증")
    class SignatureVerification {

        @Test
        @DisplayName("다른 secret으로 만든 provider는 토큰을 INVALID로 판정한다")
        void should_return_invalid_when_signed_with_different_secret() {
            // given
            JwtProvider signer = newProvider(SECRET_64, ACCESS_TTL, REFRESH_TTL);
            JwtProvider verifier = newProvider(OTHER_SECRET_64, ACCESS_TTL, REFRESH_TTL);
            String token = signer.generateAccessToken(1L, "user@devlog.com");

            // when
            TokenValidationResult result = verifier.validateAccessToken(token);

            // then
            assertThat(result).isEqualTo(TokenValidationResult.INVALID);
        }
    }

    // ---------------------------------------------------------------------
    // 잘못된 토큰 입력 (예외 누출 없이 INVALID)
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("잘못된 입력")
    class MalformedInput {

        @Test
        @DisplayName("null 토큰은 INVALID를 반환하며 예외가 누출되지 않는다")
        void should_return_invalid_when_token_is_null() {
            // given
            JwtProvider provider = defaultProvider();

            // when
            TokenValidationResult result = provider.validateAccessToken(null);

            // then
            assertThat(result).isEqualTo(TokenValidationResult.INVALID);
        }

        @Test
        @DisplayName("빈 문자열 토큰은 INVALID를 반환한다")
        void should_return_invalid_when_token_is_empty() {
            // given
            JwtProvider provider = defaultProvider();

            // when
            TokenValidationResult result = provider.validateAccessToken("");

            // then
            assertThat(result).isEqualTo(TokenValidationResult.INVALID);
        }

        @Test
        @DisplayName("랜덤 문자열 토큰은 INVALID를 반환한다")
        void should_return_invalid_when_token_is_garbage() {
            // given
            JwtProvider provider = defaultProvider();

            // when
            TokenValidationResult result = provider.validateAccessToken("not-a-real-jwt-token");

            // then
            assertThat(result).isEqualTo(TokenValidationResult.INVALID);
        }

        @Test
        @DisplayName("refresh 검증에도 null은 INVALID를 반환한다")
        void should_return_invalid_when_refresh_token_is_null() {
            // given
            JwtProvider provider = defaultProvider();

            // when
            TokenValidationResult result = provider.validateRefreshToken(null);

            // then
            assertThat(result).isEqualTo(TokenValidationResult.INVALID);
        }
    }
}
