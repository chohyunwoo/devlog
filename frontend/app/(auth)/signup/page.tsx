import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { SignupForm } from "@/components/auth/SignupForm";
import { hasAccessToken } from "@/lib/session";

export const metadata: Metadata = {
  title: "회원가입 | DevLog",
};

export default async function SignupPage() {
  if (await hasAccessToken()) {
    redirect("/");
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-semibold text-zinc-900 dark:text-zinc-50">
          회원가입
        </h1>
        <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
          DevLog 계정을 만들어 포스트와 개발 일기를 작성하세요.
        </p>
      </div>
      <SignupForm />
    </div>
  );
}
