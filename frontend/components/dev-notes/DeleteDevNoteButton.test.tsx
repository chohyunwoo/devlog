import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

const deleteDevNoteActionMock = vi.fn<(formData: FormData) => Promise<void>>();

vi.mock("@/app/actions/dev-notes", () => ({
  deleteDevNoteAction: (formData: FormData) =>
    deleteDevNoteActionMock(formData),
}));

import { DeleteDevNoteButton } from "./DeleteDevNoteButton";

describe("<DeleteDevNoteButton />", () => {
  it("최초에는 '삭제' 버튼만 노출한다", () => {
    render(<DeleteDevNoteButton noteId={7} />);
    expect(
      screen.getByRole("button", { name: "삭제" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "삭제 확인" }),
    ).not.toBeInTheDocument();
  });

  it("삭제 클릭 시 확인 UI 로 전환되고, 취소 클릭 시 다시 돌아온다", async () => {
    const user = userEvent.setup();
    render(<DeleteDevNoteButton noteId={7} />);

    await user.click(screen.getByRole("button", { name: "삭제" }));
    expect(
      screen.getByRole("button", { name: "삭제 확인" }),
    ).toBeInTheDocument();
    expect(screen.getByText("정말 삭제하시겠습니까?")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "취소" }));
    expect(screen.getByRole("button", { name: "삭제" })).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "삭제 확인" }),
    ).not.toBeInTheDocument();
  });

  it("확인 클릭 시 noteId 를 hidden field 로 담아 action 에 제출한다", async () => {
    deleteDevNoteActionMock.mockResolvedValue(undefined);
    const user = userEvent.setup();
    render(<DeleteDevNoteButton noteId={7} />);

    await user.click(screen.getByRole("button", { name: "삭제" }));
    await user.click(screen.getByRole("button", { name: "삭제 확인" }));

    expect(deleteDevNoteActionMock).toHaveBeenCalledTimes(1);
    const formData = deleteDevNoteActionMock.mock.calls[0][0];
    expect(formData.get("noteId")).toBe("7");
  });
});
