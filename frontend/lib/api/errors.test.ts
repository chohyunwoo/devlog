import { describe, expect, it } from "vitest";

import { ApiError, NetworkError } from "./errors";

describe("ApiError", () => {
  it("백엔드 에러 바디의 code/message/fieldErrors 를 그대로 보존한다", () => {
    const error = new ApiError(400, {
      code: "INVALID_REQUEST",
      message: "요청이 올바르지 않습니다.",
      fieldErrors: [
        { field: "email", message: "must not be blank" },
        { field: "password", message: "must be at least 8" },
      ],
    });

    expect(error).toBeInstanceOf(Error);
    expect(error.name).toBe("ApiError");
    expect(error.status).toBe(400);
    expect(error.code).toBe("INVALID_REQUEST");
    expect(error.message).toBe("요청이 올바르지 않습니다.");
    expect(error.fieldErrors).toHaveLength(2);
    expect(error.fieldErrors[0]).toEqual({
      field: "email",
      message: "must not be blank",
    });
  });

  it("fieldErrors 가 생략돼도 빈 배열로 노출된다", () => {
    const error = new ApiError(409, {
      code: "DUPLICATE_EMAIL",
      message: "이미 사용 중인 이메일입니다.",
    });

    expect(error.fieldErrors).toEqual([]);
  });
});

describe("NetworkError", () => {
  it("원인 에러를 cause 로 래핑하며 고정 메시지를 노출한다", () => {
    const cause = new Error("ECONNREFUSED");
    const error = new NetworkError(cause);

    expect(error).toBeInstanceOf(Error);
    expect(error.name).toBe("NetworkError");
    expect(error.message).toBe("백엔드 서버에 연결할 수 없습니다.");
    expect(error.cause).toBe(cause);
  });
});
