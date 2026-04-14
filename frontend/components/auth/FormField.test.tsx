import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { FormField } from "./FormField";

describe("<FormField />", () => {
  it("label 과 input 이 htmlFor/id 로 연결된다", () => {
    render(<FormField id="email" label="이메일" name="email" type="email" />);

    const input = screen.getByLabelText("이메일");
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute("id", "email");
    expect(input).toHaveAttribute("name", "email");
    expect(input).toHaveAttribute("type", "email");
  });

  it("hint 가 있으면 aria-describedby 로 연결된 안내문을 렌더링한다", () => {
    render(
      <FormField
        id="nickname"
        label="닉네임"
        name="nickname"
        hint="2~50자"
      />,
    );

    const input = screen.getByLabelText("닉네임");
    expect(input).toHaveAttribute("aria-describedby", "nickname-hint");
    expect(input).not.toHaveAttribute("aria-invalid");

    const hint = screen.getByText("2~50자");
    expect(hint).toHaveAttribute("id", "nickname-hint");
  });

  it("error 가 있으면 aria-invalid=true 와 에러 메시지를 연결한다", () => {
    render(
      <FormField
        id="email"
        label="이메일"
        name="email"
        error="이미 사용 중인 이메일입니다."
      />,
    );

    const input = screen.getByLabelText("이메일");
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveAttribute("aria-describedby", "email-error");

    const errorMessage = screen.getByText("이미 사용 중인 이메일입니다.");
    expect(errorMessage).toHaveAttribute("id", "email-error");
  });

  it("error 가 있으면 hint 대신 error 메시지를 우선 노출한다", () => {
    render(
      <FormField
        id="password"
        label="비밀번호"
        name="password"
        hint="8~72자"
        error="너무 짧습니다."
      />,
    );

    expect(screen.getByText("너무 짧습니다.")).toBeInTheDocument();
    expect(screen.queryByText("8~72자")).not.toBeInTheDocument();
    const input = screen.getByLabelText("비밀번호");
    expect(input).toHaveAttribute("aria-describedby", "password-error");
  });

  it("defaultValue 와 placeholder 같은 input 속성이 그대로 전달된다", () => {
    render(
      <FormField
        id="email"
        label="이메일"
        name="email"
        defaultValue="user@devlog.com"
        placeholder="you@devlog.com"
        maxLength={254}
        required
      />,
    );

    const input = screen.getByLabelText("이메일") as HTMLInputElement;
    expect(input.value).toBe("user@devlog.com");
    expect(input).toHaveAttribute("placeholder", "you@devlog.com");
    expect(input).toHaveAttribute("maxlength", "254");
    expect(input).toBeRequired();
  });
});
