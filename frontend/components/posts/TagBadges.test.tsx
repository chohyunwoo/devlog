import { render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";

vi.mock("next/link", () => ({
  default: ({
    href,
    children,
    ...rest
  }: {
    href: string;
    children: ReactNode;
  } & Record<string, unknown>) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

import { TagBadges } from "./TagBadges";

describe("<TagBadges />", () => {
  it("태그가 없으면 아무것도 렌더링하지 않는다", () => {
    const { container } = render(<TagBadges tags={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("#prefix 가 붙은 태그들을 나열한다", () => {
    render(<TagBadges tags={["java", "spring"]} />);
    const list = screen.getByRole("list", { name: "태그" });
    expect(list).toBeInTheDocument();
    expect(screen.getByText("#java")).toBeInTheDocument();
    expect(screen.getByText("#spring")).toBeInTheDocument();
  });

  it("기본값에서는 태그가 span 으로만 렌더링되고 링크가 아니다", () => {
    render(<TagBadges tags={["java"]} />);
    expect(
      screen.queryByRole("link", { name: "#java" }),
    ).not.toBeInTheDocument();
  });

  it("linkToFilter 가 true 면 /posts?tag= 쿼리 링크를 렌더링한다", () => {
    render(<TagBadges tags={["spring boot"]} linkToFilter />);
    const link = screen.getByRole("link", { name: "#spring boot" });
    expect(link).toHaveAttribute("href", "/posts?tag=spring%20boot");
  });
});
