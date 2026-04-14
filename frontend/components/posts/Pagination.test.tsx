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

import { Pagination } from "./Pagination";

const buildHref = (p: number) => `/posts?page=${p}`;

describe("<Pagination />", () => {
  it("총 1페이지 이하면 아무것도 렌더링하지 않는다", () => {
    const { container } = render(
      <Pagination
        page={0}
        totalPages={1}
        first
        last
        buildHref={buildHref}
      />,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("첫 페이지면 이전은 비활성, 다음은 활성 링크", () => {
    render(
      <Pagination
        page={0}
        totalPages={3}
        first
        last={false}
        buildHref={buildHref}
      />,
    );
    const prev = screen.getByText("이전");
    expect(prev).toHaveAttribute("aria-disabled", "true");
    expect(prev.tagName).toBe("SPAN");

    const next = screen.getByRole("link", { name: "다음" });
    expect(next).toHaveAttribute("href", "/posts?page=1");
    expect(next).toHaveAttribute("rel", "next");
    expect(screen.getByText("1 / 3")).toBeInTheDocument();
  });

  it("마지막 페이지면 다음은 비활성, 이전은 활성 링크", () => {
    render(
      <Pagination
        page={2}
        totalPages={3}
        first={false}
        last
        buildHref={buildHref}
      />,
    );
    const prev = screen.getByRole("link", { name: "이전" });
    expect(prev).toHaveAttribute("href", "/posts?page=1");
    expect(prev).toHaveAttribute("rel", "prev");

    const next = screen.getByText("다음");
    expect(next).toHaveAttribute("aria-disabled", "true");
    expect(next.tagName).toBe("SPAN");
    expect(screen.getByText("3 / 3")).toBeInTheDocument();
  });

  it("중간 페이지면 이전/다음 둘 다 링크", () => {
    render(
      <Pagination
        page={1}
        totalPages={3}
        first={false}
        last={false}
        buildHref={buildHref}
      />,
    );
    expect(screen.getByRole("link", { name: "이전" })).toHaveAttribute(
      "href",
      "/posts?page=0",
    );
    expect(screen.getByRole("link", { name: "다음" })).toHaveAttribute(
      "href",
      "/posts?page=2",
    );
  });
});
