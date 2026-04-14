import Link from "next/link";
import type { ReactNode } from "react";

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-zinc-50 px-4 py-12 dark:bg-black">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <Link
            href="/"
            className="text-2xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50"
          >
            DevLog
          </Link>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            개발 일기와 미니 블로그
          </p>
        </div>
        <div className="rounded-lg border border-zinc-200 bg-white p-8 shadow-sm dark:border-zinc-800 dark:bg-zinc-950">
          {children}
        </div>
      </div>
    </div>
  );
}
