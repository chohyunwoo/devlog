import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";

import type { DevNoteFormState } from "@/app/actions/dev-note-form-state";

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

const createDevNoteActionMock = vi.fn<
  (prev: DevNoteFormState, formData: FormData) => Promise<DevNoteFormState>
>();
const updateDevNoteActionMock = vi.fn<
  (
    noteId: number,
    prev: DevNoteFormState,
    formData: FormData,
  ) => Promise<DevNoteFormState>
>();

vi.mock("@/app/actions/dev-notes", () => ({
  createDevNoteAction: (prev: DevNoteFormState, formData: FormData) =>
    createDevNoteActionMock(prev, formData),
  updateDevNoteAction: (
    noteId: number,
    prev: DevNoteFormState,
    formData: FormData,
  ) => updateDevNoteActionMock(noteId, prev, formData),
}));

import { DevNoteForm } from "./DevNoteForm";

function emptyState(): DevNoteFormState {
  return {
    fieldErrors: {},
    formError: null,
    values: { title: "", content: "" },
  };
}

describe("<DevNoteForm /> create 모드", () => {
  it("제목/본문 필드와 작성 버튼을 렌더링한다", () => {
    render(<DevNoteForm mode="create" />);

    expect(screen.getByLabelText("제목")).toBeInTheDocument();
    expect(screen.getByLabelText("본문")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "작성 완료" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "취소" })).toHaveAttribute(
      "href",
      "/dev-notes",
    );
  });

  it("제출 시 createDevNoteAction 에 FormData 를 전달한다", async () => {
    createDevNoteActionMock.mockResolvedValue(emptyState());
    const user = userEvent.setup();

    render(<DevNoteForm mode="create" />);

    await user.type(screen.getByLabelText("제목"), "2026-04-18 TIL");
    await user.type(screen.getByLabelText("본문"), "오늘 배운 것");
    await user.click(screen.getByRole("button", { name: "작성 완료" }));

    await waitFor(() => {
      expect(createDevNoteActionMock).toHaveBeenCalledTimes(1);
    });
    const [, formData] = createDevNoteActionMock.mock.calls[0];
    expect(formData.get("title")).toBe("2026-04-18 TIL");
    expect(formData.get("content")).toBe("오늘 배운 것");
  });

  it("formError 가 돌아오면 role=alert 배너가 노출된다", async () => {
    createDevNoteActionMock.mockResolvedValue({
      fieldErrors: {},
      formError: "백엔드 서버에 연결할 수 없습니다.",
      values: { title: "t", content: "c" },
    });
    const user = userEvent.setup();

    render(<DevNoteForm mode="create" />);

    await user.type(screen.getByLabelText("제목"), "t");
    await user.type(screen.getByLabelText("본문"), "c");
    await user.click(screen.getByRole("button", { name: "작성 완료" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("백엔드 서버에 연결할 수 없습니다.");
  });
});

describe("<DevNoteForm /> edit 모드", () => {
  it("initialValues 를 필드에 채우고 버튼 라벨이 '수정 완료' 가 된다", () => {
    render(
      <DevNoteForm
        mode="edit"
        noteId={42}
        initialValues={{ title: "기존 제목", content: "기존 본문" }}
      />,
    );

    expect(screen.getByLabelText("제목")).toHaveValue("기존 제목");
    expect(screen.getByLabelText("본문")).toHaveValue("기존 본문");
    expect(
      screen.getByRole("button", { name: "수정 완료" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "취소" })).toHaveAttribute(
      "href",
      "/dev-notes/42",
    );
  });

  it("제출 시 bind 된 noteId 와 함께 updateDevNoteAction 을 호출한다", async () => {
    updateDevNoteActionMock.mockResolvedValue({
      fieldErrors: {},
      formError: null,
      values: { title: "새 제목", content: "기존 본문" },
    });
    const user = userEvent.setup();

    render(
      <DevNoteForm
        mode="edit"
        noteId={42}
        initialValues={{ title: "기존 제목", content: "기존 본문" }}
      />,
    );

    const titleInput = screen.getByLabelText("제목");
    await user.clear(titleInput);
    await user.type(titleInput, "새 제목");
    await user.click(screen.getByRole("button", { name: "수정 완료" }));

    await waitFor(() => {
      expect(updateDevNoteActionMock).toHaveBeenCalledTimes(1);
    });
    const [noteId, , formData] = updateDevNoteActionMock.mock.calls[0];
    expect(noteId).toBe(42);
    expect(formData.get("title")).toBe("새 제목");
    expect(formData.get("content")).toBe("기존 본문");
  });
});
