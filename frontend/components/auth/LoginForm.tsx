"use client";

import Link from "next/link";
import { useActionState } from "react";

import { loginAction } from "@/app/actions/auth";
import { initialAuthFormState } from "@/app/actions/auth-form-state";

import { FormField } from "./FormField";

export function LoginForm() {
  const [state, action, pending] = useActionState(
    loginAction,
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
        id="password"
        label="비밀번호"
        name="password"
        type="password"
        autoComplete="current-password"
        required
        maxLength={128}
        placeholder="••••••••"
        error={state.fieldErrors.password}
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
        {pending ? "로그인 중..." : "로그인"}
      </button>

      <p className="text-center text-sm text-zinc-600 dark:text-zinc-400">
        아직 계정이 없으신가요?{" "}
        <Link
          href="/signup"
          className="font-medium text-zinc-900 underline-offset-4 hover:underline dark:text-zinc-50"
        >
          회원가입
        </Link>
      </p>
    </form>
  );
}
