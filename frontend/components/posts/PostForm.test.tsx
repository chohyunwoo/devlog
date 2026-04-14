import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";

import type { PostFormState } from "@/app/actions/post-form-state";

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

const createPostActionMock = vi.fn<
  (prev: PostFormState, formData: FormData) => Promise<PostFormState>
>();

vi.mock("@/app/actions/posts", () => ({
  createPostAction: (prev: PostFormState, formData: FormData) =>
    createPostActionMock(prev, formData),
}));

import { PostForm } from "./PostForm";

function initialState(): PostFormState {
  return {
    fieldErrors: {},
    formError: null,
    values: { title: "", content: "", tags: "" },
  };
}

describe("<PostForm />", () => {
  it("제목/본문/태그 필드와 작성 버튼을 렌더링한다", () => {
    render(<PostForm />);

    expect(screen.getByLabelText("제목")).toBeInTheDocument();
    expect(screen.getByLabelText("본문")).toBeInTheDocument();
    expect(screen.getByLabelText("태그")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "작성 완료" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "취소" })).toHaveAttribute(
      "href",
      "/posts",
    );
  });

  it("제출 시 FormData 로 title/content/tags 를 전달한다", async () => {
    createPostActionMock.mockResolvedValue(initialState());
    const user = userEvent.setup();

    render(<PostForm />);

    await user.type(screen.getByLabelText("제목"), "Spring Boot 체험기");
    await user.type(
      screen.getByLabelText("본문"),
      "본문 내용입니다.\n두 번째 줄.",
    );
    await user.type(screen.getByLabelText("태그"), "java, spring");
    await user.click(screen.getByRole("button", { name: "작성 완료" }));

    await waitFor(() => {
      expect(createPostActionMock).toHaveBeenCalledTimes(1);
    });

    const [, formData] = createPostActionMock.mock.calls[0];
    expect(formData.get("title")).toBe("Spring Boot 체험기");
    expect(formData.get("content")).toBe("본문 내용입니다.\n두 번째 줄.");
    expect(formData.get("tags")).toBe("java, spring");
  });

  it("title field error 가 돌아오면 해당 input 이 aria-invalid 가 된다", async () => {
    createPostActionMock.mockResolvedValue({
      fieldErrors: { title: "제목은 필수입니다." },
      formError: null,
      values: { title: "", content: "본문", tags: "" },
    });
    const user = userEvent.setup();

    render(<PostForm />);

    await user.type(screen.getByLabelText("제목"), " ");
    await user.type(screen.getByLabelText("본문"), "본문");
    await user.click(screen.getByRole("button", { name: "작성 완료" }));

    await waitFor(() => {
      expect(screen.getByText("제목은 필수입니다.")).toBeInTheDocument();
    });
    expect(screen.getByLabelText("제목")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
  });

  it("content field error 가 돌아오면 textarea 에 연결된다", async () => {
    createPostActionMock.mockResolvedValue({
      fieldErrors: { content: "본문은 필수입니다." },
      formError: null,
      values: { title: "t", content: "", tags: "" },
    });
    const user = userEvent.setup();

    render(<PostForm />);

    await user.type(screen.getByLabelText("제목"), "t");
    await user.click(screen.getByRole("button", { name: "작성 완료" }));

    await waitFor(() => {
      expect(screen.getByText("본문은 필수입니다.")).toBeInTheDocument();
    });
    const textarea = screen.getByLabelText("본문");
    expect(textarea).toHaveAttribute("aria-invalid", "true");
    expect(textarea).toHaveAttribute("aria-describedby", "content-error");
  });

  it("formError 가 돌아오면 role=alert 배너가 노출된다", async () => {
    createPostActionMock.mockResolvedValue({
      fieldErrors: {},
      formError: "백엔드 서버에 연결할 수 없습니다.",
      values: { title: "t", content: "c", tags: "" },
    });
    const user = userEvent.setup();

    render(<PostForm />);

    await user.type(screen.getByLabelText("제목"), "t");
    await user.type(screen.getByLabelText("본문"), "c");
    await user.click(screen.getByRole("button", { name: "작성 완료" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("백엔드 서버에 연결할 수 없습니다.");
  });

  it("action 이 진행 중이면 submit 버튼이 disabled '작성 중...' 을 노출한다", async () => {
    createPostActionMock.mockImplementation(
      () => new Promise<PostFormState>(() => {}),
    );
    const user = userEvent.setup();

    render(<PostForm />);

    await user.type(screen.getByLabelText("제목"), "t");
    await user.type(screen.getByLabelText("본문"), "c");
    await user.click(screen.getByRole("button", { name: "작성 완료" }));

    const pending = await screen.findByRole("button", { name: "작성 중..." });
    expect(pending).toBeDisabled();
  });
});
