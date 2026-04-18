import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { DevNoteForm } from "@/components/dev-notes/DevNoteForm";
import { hasAccessToken } from "@/lib/session";

export const metadata: Metadata = {
  title: "새 개발 일기 | DevLog",
};

export default async function NewDevNotePage() {
  if (!(await hasAccessToken())) {
    redirect("/login?next=/dev-notes/new");
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-6 py-10">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50">
          새 개발 일기
        </h1>
        <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
          본인만 볼 수 있는 비공개 기록입니다.
        </p>
      </header>
      <DevNoteForm mode="create" />
    </div>
  );
}
