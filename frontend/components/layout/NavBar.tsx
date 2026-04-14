import Link from "next/link";

import { logoutAction } from "@/app/actions/auth";
import { hasAccessToken } from "@/lib/session";

export async function NavBar() {
  const authed = await hasAccessToken();

  return (
    <header className="sticky top-0 z-10 border-b border-zinc-200 bg-white/80 backdrop-blur dark:border-zinc-800 dark:bg-black/80">
      <nav className="mx-auto flex h-14 w-full max-w-5xl items-center gap-6 px-6 text-sm">
        <Link
          href="/"
          className="text-base font-semibold tracking-tight text-zinc-900 dark:text-zinc-50"
        >
          DevLog
        </Link>
        <div className="flex items-center gap-4 text-zinc-600 dark:text-zinc-400">
          <Link
            href="/posts"
            className="transition-colors hover:text-zinc-900 dark:hover:text-zinc-50"
          >
            포스트
          </Link>
        </div>
        <div className="ml-auto flex items-center gap-3">
          {authed ? (
            <form action={logoutAction}>
              <button
                type="submit"
                className="flex h-9 items-center justify-center rounded-md border border-zinc-300 bg-white px-3 text-xs font-medium text-zinc-900 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-50 dark:hover:bg-zinc-900"
              >
                로그아웃
              </button>
            </form>
          ) : (
            <>
              <Link
                href="/login"
                className="text-zinc-600 transition-colors hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-50"
              >
                로그인
              </Link>
              <Link
                href="/signup"
                className="flex h-9 items-center justify-center rounded-md bg-zinc-900 px-3 text-xs font-medium text-zinc-50 transition-colors hover:bg-zinc-800 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200"
              >
                회원가입
              </Link>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}
