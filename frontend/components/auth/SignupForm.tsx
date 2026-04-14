"use client";

import Link from "next/link";
import { useActionState } from "react";

import { signupAction } from "@/app/actions/auth";
import { initialAuthFormState } from "@/app/actions/auth-form-state";

import { FormField } from "./FormField";

export function SignupForm() {
  const [state, action, pending] = useActionState(
    signupAction,
    initialAuthFormState,
  );

  return (
    <form action={action} className="flex flex-col gap-5" noValidate>
      <FormField
        id="email"
        label="이메일"
        name="email"
        type="email"
        autoComplete="email"
        required
        maxLength={254}
        placeholder="you@devlog.com"
        defaultValue={state.values.email}
        error={state.fieldErrors.email}
      />

      <FormField
        id="nickname"
        label="닉네임"
        name="nickname"
        type="text"
        autoComplete="nickname"
        required
        minLength={2}
        maxLength={50}
        placeholder="devuser"
        defaultValue={state.values.nickname}
        error={state.fieldErrors.nickname}
        hint="2~50자"
      />

      <FormField
        id="password"
        label="비밀번호"
        name="password"
        type="password"
        autoComplete="new-password"
        required
        minLength={8}
        maxLength={72}
        placeholder="••••••••"
        error={state.fieldErrors.password}
        hint="8~72자"
      />

      {state.formError ? (
        <div
          role="alert"
          className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/40 dark:text-red-300"
        >
          {state.formError}
        </div>
      ) : null}

      <button
        type="submit"
        disabled={pending}
        className="h-11 rounded-md bg-zinc-900 text-sm font-medium text-zinc-50 transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200"
      >
        {pending ? "가입 중..." : "회원가입"}
      </button>

      <p className="text-center text-sm text-zinc-600 dark:text-zinc-400">
        이미 계정이 있으신가요?{" "}
        <Link
          href="/login"
          className="font-medium text-zinc-900 underline-offset-4 hover:underline dark:text-zinc-50"
        >
          로그인
        </Link>
      </p>
    </form>
  );
}
