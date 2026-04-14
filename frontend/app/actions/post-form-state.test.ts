import { describe, expect, it } from "vitest";

import { parseTagsInput } from "./post-form-state";

describe("parseTagsInput", () => {
  it("쉼표로 분리하고 앞뒤 공백을 제거한다", () => {
    expect(parseTagsInput("java, spring, boot")).toEqual([
      "java",
      "spring",
      "boot",
    ]);
  });

  it("빈 토큰과 공백 전용 토큰을 제거한다", () => {
    expect(parseTagsInput("java,, ,spring , ")).toEqual(["java", "spring"]);
  });

  it("빈 문자열이면 빈 배열을 돌려준다", () => {
    expect(parseTagsInput("")).toEqual([]);
    expect(parseTagsInput("   ")).toEqual([]);
  });

  it("단일 태그도 배열로 감싼다", () => {
    expect(parseTagsInput("kotlin")).toEqual(["kotlin"]);
  });

  it("중복 태그는 한 번만 남는다", () => {
    expect(parseTagsInput("java, spring, java, Spring")).toEqual([
      "java",
      "spring",
      "Spring",
    ]);
  });
});
