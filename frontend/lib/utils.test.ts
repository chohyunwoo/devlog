import { describe, expect, it } from "vitest";

import { cn } from "./utils";

describe("cn", () => {
  it("falsy 값을 버리고 공백으로 이어붙인다", () => {
    expect(cn("a", "b", "c")).toBe("a b c");
    expect(cn("a", false, "b", null, "c", undefined)).toBe("a b c");
  });

  it("모두 falsy 면 빈 문자열을 반환한다", () => {
    expect(cn(false, null, undefined)).toBe("");
  });
});
