import Link from "next/link";

import { hasAccessToken } from "@/lib/session";

export default async function Home() {
  const authed = await hasAccessToken();

  return (
    <div className="flex flex-1 flex-col items-center justify-center bg-zinc-50 px-6 py-16 dark:bg-black">
      <section className="flex w-full max-w-2xl flex-col items-center gap-10 text-center">
        <div className="flex flex-col gap-3">
          <span className="text-xs font-medium uppercase tracking-widest text-zinc-500 dark:text-zinc-400">
            Personal Dev Blog
          </span>
          <h1 className="text-4xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50 sm:text-5xl">
            DevLog
          </h1>
          <p className="text-base text-zinc-600 dark:text-zinc-400 sm:text-lg">
            개발 일기와 미니 블로그를 한 곳에. 공개 포스트로 기록하고, 비공개
            노트로 회고하세요.
          </p>
        </div>

        <div className="flex flex-col items-center gap-3 sm:flex-row">
          <Link
            href="/posts"
            className="flex h-11 w-48 items-center justify-center rounded-md bg-zinc-900 text-sm font-medium text-zinc-50 transition-colors hover:bg-zinc-800 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200"
          >
            포스트 둘러보기
          </Link>
          {authed ? (
            <Link
              href="/posts/new"
              className="flex h-11 w-48 items-center justify-center rounded-md border border-zinc-300 bg-white text-sm font-medium text-zinc-900 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-50 dark:hover:bg-zinc-900"
            >
              새 포스트 작성
            </Link>
          ) : (
            <Link
              href="/signup"
              className="flex h-11 w-48 items-center justify-center rounded-md border border-zinc-300 bg-white text-sm font-medium text-zinc-900 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-50 dark:hover:bg-zinc-900"
            >
              회원가입
            </Link>
          )}
        </div>
      </section>
    </div>
  );
}
