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

const signupActionMock = vi.fn<
  (prev: AuthFormState, formData: FormData) => Promise<AuthFormState>
>();

vi.mock("@/app/actions/auth", () => ({
  signupAction: (prev: AuthFormState, formData: FormData) =>
    signupActionMock(prev, formData),
}));

import { SignupForm } from "./SignupForm";

function initialState(): AuthFormState {
  return { fieldErrors: {}, formError: null, values: {} };
}

describe("<SignupForm />", () => {
  it("이메일/닉네임/비밀번호 3개 필드와 회원가입 버튼을 렌더링한다", () => {
    render(<SignupForm />);

    expect(screen.getByLabelText("이메일")).toBeInTheDocument();
    expect(screen.getByLabelText("닉네임")).toBeInTheDocument();
    expect(screen.getByLabelText("비밀번호")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "회원가입" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "로그인" })).toHaveAttribute(
      "href",
      "/login",
    );
  });

  it("폼 제출 시 signupAction 에 세 필드 값이 FormData 로 전달된다", async () => {
    signupActionMock.mockResolvedValue(initialState());
    const user = userEvent.setup();

    render(<SignupForm />);

    await user.type(
      screen.getByLabelText("이메일"),
      "new@devlog.com",
    );
    await user.type(screen.getByLabelText("닉네임"), "newbie");
    await user.type(
      screen.getByLabelText("비밀번호"),
      "password1234",
    );
    await user.click(screen.getByRole("button", { name: "회원가입" }));

    await waitFor(() => {
      expect(signupActionMock).toHaveBeenCalledTimes(1);
    });

    const [, formDataArg] = signupActionMock.mock.calls[0];
    expect(formDataArg.get("email")).toBe("new@devlog.com");
    expect(formDataArg.get("nickname")).toBe("newbie");
    expect(formDataArg.get("password")).toBe("password1234");
  });

  it("action 이 닉네임 중복 field error 를 반환하면 해당 input 이 aria-invalid 가 된다", async () => {
    signupActionMock.mockResolvedValue({
      fieldErrors: { nickname: "이미 사용 중인 닉네임입니다." },
      formError: null,
      values: { email: "new@devlog.com", nickname: "taken" },
    });
    const user = userEvent.setup();

    render(<SignupForm />);

    await user.type(
      screen.getByLabelText("이메일"),
      "new@devlog.com",
    );
    await user.type(screen.getByLabelText("닉네임"), "taken");
    await user.type(
      screen.getByLabelText("비밀번호"),
      "password1234",
    );
    await user.click(screen.getByRole("button", { name: "회원가입" }));

    await waitFor(() => {
      expect(
        screen.getByText("이미 사용 중인 닉네임입니다."),
      ).toBeInTheDocument();
    });

    const nicknameInput = screen.getByLabelText("닉네임");
    expect(nicknameInput).toHaveAttribute("aria-invalid", "true");
  });

  it("네트워크 에러처럼 formError 만 돌아오면 role=alert 로 전체 에러를 노출한다", async () => {
    signupActionMock.mockResolvedValue({
      fieldErrors: {},
      formError: "백엔드 서버에 연결할 수 없습니다.",
      values: { email: "new@devlog.com", nickname: "newbie" },
    });
    const user = userEvent.setup();

    render(<SignupForm />);

    await user.type(
      screen.getByLabelText("이메일"),
      "new@devlog.com",
    );
    await user.type(screen.getByLabelText("닉네임"), "newbie");
    await user.type(
      screen.getByLabelText("비밀번호"),
      "password1234",
    );
    await user.click(screen.getByRole("button", { name: "회원가입" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("백엔드 서버에 연결할 수 없습니다.");
  });

  it("action 이 진행 중이면 submit 버튼이 disabled 상태로 '가입 중...' 을 노출한다", async () => {
    signupActionMock.mockImplementation(
      () => new Promise<AuthFormState>(() => {}),
    );
    const user = userEvent.setup();

    render(<SignupForm />);

    await user.type(
      screen.getByLabelText("이메일"),
      "new@devlog.com",
    );
    await user.type(screen.getByLabelText("닉네임"), "newbie");
    await user.type(
      screen.getByLabelText("비밀번호"),
      "password1234",
    );
    await user.click(screen.getByRole("button", { name: "회원가입" }));

    const pendingButton = await screen.findByRole("button", {
      name: "가입 중...",
    });
    expect(pendingButton).toBeDisabled();
  });
});
