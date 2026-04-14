import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { LoginForm } from "@/components/auth/LoginForm";
import { hasAccessToken } from "@/lib/session";

export const metadata: Metadata = {
  title: "로그인 | DevLog",
};

type LoginPageProps = {
  searchParams: Promise<{ signup?: string }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  if (await hasAccessToken()) {
    redirect("/");
  }

  const { signup } = await searchParams;
  const showSignupSuccess = signup === "success";

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-semibold text-zinc-900 dark:text-zinc-50">
          로그인
        </h1>
        <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
          DevLog 계정으로 로그인하세요.
        </p>
      </div>
      {showSignupSuccess ? (
        <div
          role="status"
          className="rounded-md border border-emerald-300 bg-emerald-50 px-3 py-2 text-sm text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-200"
        >
          회원가입이 완료되었습니다. 로그인해주세요.
        </div>
      ) : null}
      <LoginForm />
    </div>
  );
}
