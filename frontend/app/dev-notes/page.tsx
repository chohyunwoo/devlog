import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";

import { DevNoteCard } from "@/components/dev-notes/DevNoteCard";
import { Pagination } from "@/components/posts/Pagination";
import { ApiError, toErrorMessage } from "@/lib/api/errors";
import {
  listDevNotes,
  type DevNotePageResponse,
} from "@/lib/api/dev-notes";
import { hasAccessToken } from "@/lib/session";

export const metadata: Metadata = {
  title: "개발 일기 | DevLog",
};

type DevNotesPageProps = {
  searchParams: Promise<{
    page?: string;
    error?: string;
  }>;
};

const DEFAULT_PAGE_SIZE = 20;

export default async function DevNotesPage({ searchParams }: DevNotesPageProps) {
  if (!(await hasAccessToken())) {
    redirect("/login?next=/dev-notes");
  }

  const params = await searchParams;
  const page = parsePageNumber(params.page);

  let result: DevNotePageResponse | null = null;
  let errorMessage: string | null = null;

  try {
    result = await listDevNotes({ page, size: DEFAULT_PAGE_SIZE });
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      redirect("/login?next=/dev-notes");
    }
    errorMessage = toErrorMessage(error, "개발 일기 목록을 불러오지 못했습니다.");
  }

  if (result && result.totalPages > 0 && page >= result.totalPages) {
    redirect(buildHref(result.totalPages - 1));
  }

  const notice =
    params.error === "not_found"
      ? "해당 개발 일기를 찾을 수 없습니다."
      : null;

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-8 px-6 py-10">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50">
            개발 일기
          </h1>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            본인만 볼 수 있는 비공개 TIL · 회고 공간입니다.
          </p>
        </div>
        <Link
          href="/dev-notes/new"
          className="flex h-10 items-center justify-center rounded-md bg-zinc-900 px-4 text-sm font-medium text-zinc-50 transition-colors hover:bg-zinc-800 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200"
        >
          새 일기 작성
        </Link>
      </header>

      {notice ? (
        <div
          role="status"
          className="rounded-md border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:border-amber-900 dark:bg-amber-950/40 dark:text-amber-300"
        >
          {notice}
        </div>
      ) : null}

      {errorMessage ? (
        <div
          role="alert"
          className="rounded-md border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/40 dark:text-red-300"
        >
          {errorMessage}
        </div>
      ) : result && result.content.length === 0 ? (
        <p className="rounded-md border border-dashed border-zinc-300 bg-zinc-50 px-4 py-12 text-center text-sm text-zinc-500 dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-400">
          아직 작성한 개발 일기가 없습니다.
        </p>
      ) : result ? (
        <>
          <ul className="flex flex-col gap-4">
            {result.content.map((note) => (
              <li key={note.id}>
                <DevNoteCard note={note} />
              </li>
            ))}
          </ul>
          <Pagination
            page={result.page}
            totalPages={result.totalPages}
            first={result.first}
            last={result.last}
            buildHref={buildHref}
          />
        </>
      ) : null}
    </div>
  );
}

function parsePageNumber(raw: string | undefined): number {
  if (!raw) return 0;
  const n = Number.parseInt(raw, 10);
  return Number.isFinite(n) && n >= 0 ? n : 0;
}

function buildHref(page: number): string {
  if (page <= 0) return "/dev-notes";
  const params = new URLSearchParams();
  params.set("page", String(page));
  return `/dev-notes?${params.toString()}`;
}
