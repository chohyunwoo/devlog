import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { PostForm } from "@/components/posts/PostForm";
import { hasAccessToken } from "@/lib/session";

export const metadata: Metadata = {
  title: "새 포스트 | DevLog",
};

export default async function NewPostPage() {
  if (!(await hasAccessToken())) {
    redirect("/login?next=/posts/new");
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-6 py-10">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50">
          새 포스트 작성
        </h1>
        <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
          공개 블로그에 게시될 내용입니다.
        </p>
      </header>
      <PostForm />
    </div>
  );
}
