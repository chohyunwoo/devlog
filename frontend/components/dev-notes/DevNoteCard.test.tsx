import { render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";

import type { DevNoteSummary } from "@/lib/api/dev-notes";

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

import { DevNoteCard } from "./DevNoteCard";

const BASE_NOTE: DevNoteSummary = {
  id: 7,
  title: "2026-04-13 TIL",
  createdAt: "2026-04-13T10:20:30",
  updatedAt: "2026-04-13T10:20:30",
};

describe("<DevNoteCard />", () => {
  it("제목 링크가 상세 페이지로 연결되고 작성일을 노출한다", () => {
    render(<DevNoteCard note={BASE_NOTE} />);
    const titleLink = screen.getByRole("link", {
      name: "2026-04-13 TIL",
    });
    expect(titleLink).toHaveAttribute("href", "/dev-notes/7");
    expect(screen.getByText("2026-04-13")).toBeInTheDocument();
  });

  it("updatedAt 이 createdAt 과 다르면 수정일을 함께 노출한다", () => {
    render(
      <DevNoteCard
        note={{ ...BASE_NOTE, updatedAt: "2026-04-14T08:00:00" }}
      />,
    );
    expect(screen.getByText(/수정 2026-04-14/)).toBeInTheDocument();
  });

  it("updatedAt 이 createdAt 과 같으면 수정일은 노출되지 않는다", () => {
    render(<DevNoteCard note={BASE_NOTE} />);
    expect(screen.queryByText(/수정 /)).not.toBeInTheDocument();
  });

  it("날짜 파싱에 실패하면 원문을 그대로 보여준다", () => {
    render(
      <DevNoteCard
        note={{ ...BASE_NOTE, createdAt: "not-a-date", updatedAt: "not-a-date" }}
      />,
    );
    expect(screen.getByText("not-a-date")).toBeInTheDocument();
  });
});
