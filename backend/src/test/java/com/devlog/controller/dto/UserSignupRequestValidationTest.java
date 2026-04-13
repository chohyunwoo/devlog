package com.devlog.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * UserSignupRequest의 Bean Validation 제약을 검증하는 단위 테스트.
 * Spring context 없이 Validator를 직접 생성해 사용한다.
 */
class UserSignupRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    private static String repeat(char c, int length) {
        char[] buf = new char[length];
        for (int i = 0; i < length; i++) {
            buf[i] = c;
        }
        return new String(buf);
    }

    @Test
    @DisplayName("유효한 email/password/nickname 조합은 위반을 발생시키지 않는다")
    void should_haveNoViolations_when_allFieldsValid() {
        // given
        UserSignupRequest request = new UserSignupRequest(
                "tester@devlog.com",
                "password1234",
                "tester"
        );

        // when
        Set<ConstraintViolation<UserSignupRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("경계값 이내(password 8자, nickname 2자)는 유효하다")
    void should_acceptBoundaryValues_when_onMinEdge() {
        // given
        UserSignupRequest request = new UserSignupRequest(
                "a@b.co",
                "12345678",   // 8자
                "ab"          // 2자
        );

        // when
        Set<ConstraintViolation<UserSignupRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("경계값 상한(password 72자, nickname 50자)은 유효하다")
    void should_acceptBoundaryValues_when_onMaxEdge() {
        // given
        UserSignupRequest request = new UserSignupRequest(
                "tester@devlog.com",
                repeat('p', 72),
                repeat('n', 50)
        );

        // when
        Set<ConstraintViolation<UserSignupRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    static Stream<Arguments> invalidRequests() {
        String validEmail = "tester@devlog.com";
        String validPassword = "password1234";
        String validNickname = "tester";

        // email 본문 길이 계산: "@devlog.com" = 11자. 로컬부 244자 + 11자 = 255자.
        String tooLongEmail = repeat('a', 244) + "@devlog.com";
        // 1자 nickname
        String oneCharNickname = "a";
        // 51자 nickname
        String tooLongNickname = repeat('n', 51);
        // 7자 password
        String tooShortPassword = "1234567";
        // 73자 password
        String tooLongPassword = repeat('p', 73);

        return Stream.of(
                // email 실패 케이스
                Arguments.of("blank email", new UserSignupRequest("", validPassword, validNickname), "email"),
                Arguments.of("null email", new UserSignupRequest(null, validPassword, validNickname), "email"),
                Arguments.of("malformed email", new UserSignupRequest("not-an-email", validPassword, validNickname), "email"),
                Arguments.of("email exceeding 254", new UserSignupRequest(tooLongEmail, validPassword, validNickname), "email"),

                // password 실패 케이스
                Arguments.of("blank password", new UserSignupRequest(validEmail, "", validNickname), "password"),
                Arguments.of("null password", new UserSignupRequest(validEmail, null, validNickname), "password"),
                Arguments.of("7 char password", new UserSignupRequest(validEmail, tooShortPassword, validNickname), "password"),
                Arguments.of("73 char password", new UserSignupRequest(validEmail, tooLongPassword, validNickname), "password"),

                // nickname 실패 케이스
                Arguments.of("blank nickname", new UserSignupRequest(validEmail, validPassword, ""), "nickname"),
                Arguments.of("null nickname", new UserSignupRequest(validEmail, validPassword, null), "nickname"),
                Arguments.of("1 char nickname", new UserSignupRequest(validEmail, validPassword, oneCharNickname), "nickname"),
                Arguments.of("51 char nickname", new UserSignupRequest(validEmail, validPassword, tooLongNickname), "nickname")
        );
    }

    @ParameterizedTest(name = "{0} → {2} 필드에서 위반 발생")
    @MethodSource("invalidRequests")
    @DisplayName("잘못된 입력은 해당 필드에서 제약 위반을 발생시킨다")
    void should_reportViolation_when_fieldInvalid(
            String description,
            UserSignupRequest request,
            String expectedFieldWithViolation
    ) {
        // when
        Set<ConstraintViolation<UserSignupRequest>> violations = validator.validate(request);

        // then
        assertThat(violations)
                .as("[%s] %s 필드에서 최소 하나의 위반이 발생해야 한다", description, expectedFieldWithViolation)
                .isNotEmpty();
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains(expectedFieldWithViolation);
    }
}
