import { render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";

import type { PostSummary } from "@/lib/api/posts";

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

import { PostCard } from "./PostCard";

const BASE_POST: PostSummary = {
  id: 42,
  title: "Spring Boot 4 체험기",
  author: { id: 1, nickname: "devuser" },
  tags: ["java", "spring"],
  createdAt: "2026-04-13T10:20:30",
  updatedAt: "2026-04-13T10:20:30",
};

describe("<PostCard />", () => {
  it("제목 링크가 상세 페이지로 연결되고 작성자/작성일을 노출한다", () => {
    render(<PostCard post={BASE_POST} />);
    const titleLink = screen.getByRole("link", {
      name: "Spring Boot 4 체험기",
    });
    expect(titleLink).toHaveAttribute("href", "/posts/42");
    expect(screen.getByText("devuser")).toBeInTheDocument();
    expect(screen.getByText("2026-04-13")).toBeInTheDocument();
  });

  it("태그들은 필터 링크로 렌더링되고 카드 링크와 독립적이다", () => {
    render(<PostCard post={BASE_POST} />);
    const javaLink = screen.getByRole("link", { name: "#java" });
    expect(javaLink).toHaveAttribute("href", "/posts?tag=java");
    const springLink = screen.getByRole("link", { name: "#spring" });
    expect(springLink).toHaveAttribute("href", "/posts?tag=spring");
  });

  it("태그가 비어 있으면 태그 리스트는 렌더링되지 않는다", () => {
    render(<PostCard post={{ ...BASE_POST, tags: [] }} />);
    expect(screen.queryByRole("list", { name: "태그" })).not.toBeInTheDocument();
  });

  it("날짜 파싱에 실패하면 원문을 그대로 보여준다", () => {
    render(
      <PostCard
        post={{ ...BASE_POST, createdAt: "not-a-date", updatedAt: "x" }}
      />,
    );
    expect(screen.getByText("not-a-date")).toBeInTheDocument();
  });
});
