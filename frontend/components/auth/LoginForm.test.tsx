import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";

import type { AuthFormState } from "@/app/actions/auth-form-state";

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

const loginActionMock = vi.fn<
  (prev: AuthFormState, formData: FormData) => Promise<AuthFormState>
>();

vi.mock("@/app/actions/auth", () => ({
  loginAction: (prev: AuthFormState, formData: FormData) =>
    loginActionMock(prev, formData),
}));

import { LoginForm } from "./LoginForm";

function initialState(): AuthFormState {
  return { fieldErrors: {}, formError: null, values: {} };
}

describe("<LoginForm />", () => {
  it("이메일/비밀번호 필드와 로그인 버튼을 렌더링한다", () => {
    render(<LoginForm />);

    expect(screen.getByLabelText("이메일")).toBeInTheDocument();
    expect(screen.getByLabelText("비밀번호")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "로그인" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "회원가입" }),
    ).toHaveAttribute("href", "/signup");
  });

  it("폼 제출 시 loginAction 이 FormData 와 함께 호출된다", async () => {
    loginActionMock.mockResolvedValue(initialState());
    const user = userEvent.setup();

    render(<LoginForm />);

    await user.type(
      screen.getByLabelText("이메일"),
      "user@devlog.com",
    );
    await user.type(screen.getByLabelText("비밀번호"), "password1234");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => {
      expect(loginActionMock).toHaveBeenCalledTimes(1);
    });

    const [prevArg, formDataArg] = loginActionMock.mock.calls[0];
    expect(prevArg).toEqual(initialState());
    expect(formDataArg).toBeInstanceOf(FormData);
    expect(formDataArg.get("email")).toBe("user@devlog.com");
    expect(formDataArg.get("password")).toBe("password1234");
  });

  it("action 이 field error 를 반환하면 해당 input 에 에러가 표시된다", async () => {
    loginActionMock.mockResolvedValue({
      fieldErrors: { email: "이메일 형식이 아닙니다." },
      formError: null,
      values: { email: "not-an-email" },
    });
    const user = userEvent.setup();

    render(<LoginForm />);

    await user.type(screen.getByLabelText("이메일"), "not-an-email");
    await user.type(screen.getByLabelText("비밀번호"), "password1234");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => {
      expect(
        screen.getByText("이메일 형식이 아닙니다."),
      ).toBeInTheDocument();
    });

    const emailInput = screen.getByLabelText("이메일");
    expect(emailInput).toHaveAttribute("aria-invalid", "true");
    expect(emailInput).toHaveAttribute("aria-describedby", "email-error");
  });

  it("action 이 formError 를 반환하면 role=alert 배너가 노출된다", async () => {
    loginActionMock.mockResolvedValue({
      fieldErrors: {},
      formError: "이메일 또는 비밀번호가 올바르지 않습니다.",
      values: { email: "user@devlog.com" },
    });
    const user = userEvent.setup();

    render(<LoginForm />);

    await user.type(
      screen.getByLabelText("이메일"),
      "user@devlog.com",
    );
    await user.type(screen.getByLabelText("비밀번호"), "wrongpass");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(
      "이메일 또는 비밀번호가 올바르지 않습니다.",
    );
  });

  it("제출 후 에러로 되돌아와도 사용자가 입력한 이메일은 남아 있다", async () => {
    loginActionMock.mockResolvedValue({
      fieldErrors: {},
      formError: "이메일 또는 비밀번호가 올바르지 않습니다.",
      values: { email: "user@devlog.com" },
    });
    const user = userEvent.setup();

    render(<LoginForm />);

    await user.type(
      screen.getByLabelText("이메일"),
      "user@devlog.com",
    );
    await user.type(screen.getByLabelText("비밀번호"), "wrongpass");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await screen.findByRole("alert");
    expect(screen.getByLabelText("이메일")).toHaveValue("user@devlog.com");
  });

  it("action 이 진행 중이면 submit 버튼이 disabled 상태로 '로그인 중...' 을 노출한다", async () => {
    loginActionMock.mockImplementation(
      () => new Promise<AuthFormState>(() => {}),
    );
    const user = userEvent.setup();

    render(<LoginForm />);

    await user.type(
      screen.getByLabelText("이메일"),
      "user@devlog.com",
    );
    await user.type(screen.getByLabelText("비밀번호"), "password1234");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    const pendingButton = await screen.findByRole("button", {
      name: "로그인 중...",
    });
    expect(pendingButton).toBeDisabled();
  });
});
