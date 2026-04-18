import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";

import { DeleteDevNoteButton } from "@/components/dev-notes/DeleteDevNoteButton";
import { ApiError, toErrorMessage } from "@/lib/api/errors";
import { getDevNote, type DevNote } from "@/lib/api/dev-notes";
import { hasAccessToken } from "@/lib/session";

type DevNoteDetailPageProps = {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ error?: string }>;
};

type LoadResult =
  | { status: "ok"; note: DevNote }
  | { status: "not_found" }
  | { status: "error"; message: string };

async function loadDevNote(
  params: DevNoteDetailPageProps["params"],
): Promise<LoadResult> {
  const { id } = await params;
  const noteId = Number.parseInt(id, 10);
  if (!Number.isFinite(noteId) || noteId <= 0) {
    return { status: "not_found" };
  }

  try {
    const note = await getDevNote(noteId);
    return { status: "ok", note };
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return { status: "not_found" };
    }
    if (error instanceof ApiError && error.status === 401) {
      redirect(`/login?next=/dev-notes/${id}`);
    }
    return {
      status: "error",
      message: toErrorMessage(error, "개발 일기를 불러오지 못했습니다."),
    };
  }
}

export async function generateMetadata({
  params,
}: DevNoteDetailPageProps): Promise<Metadata> {
  if (!(await hasAccessToken())) {
    return { title: "개발 일기 | DevLog" };
  }
  const result = await loadDevNote(params);
  if (result.status === "ok") {
    return { title: `${result.note.title} | DevLog` };
  }
  return { title: "개발 일기 | DevLog" };
}

export default async function DevNoteDetailPage({
  params,
  searchParams,
}: DevNoteDetailPageProps) {
  const { id } = await params;
  if (!(await hasAccessToken())) {
    redirect(`/login?next=/dev-notes/${id}`);
  }

  const result = await loadDevNote(params);

  if (result.status === "not_found") {
    notFound();
  }

  const { error: errorParam } = await searchParams;
  const notice =
    errorParam === "delete_failed"
      ? "개발 일기 삭제에 실패했습니다."
      : null;

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-6 py-10">
      <div className="text-sm">
        <Link
          href="/dev-notes"
          className="text-zinc-500 underline-offset-4 hover:underline dark:text-zinc-400"
        >
          ← 목록으로
        </Link>
      </div>

      {notice ? (
        <div
          role="alert"
          className="rounded-md border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/40 dark:text-red-300"
        >
          {notice}
        </div>
      ) : null}

      {result.status === "error" ? (
        <div
          role="alert"
          className="rounded-md border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/40 dark:text-red-300"
        >
          {result.message}
        </div>
      ) : (
        <article className="flex flex-col gap-6">
          <header className="flex flex-col gap-4 border-b border-zinc-200 pb-6 dark:border-zinc-800">
            <h1 className="text-3xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50">
              {result.note.title}
            </h1>
            <div className="flex flex-wrap items-center gap-2 text-sm text-zinc-500 dark:text-zinc-400">
              <time dateTime={result.note.createdAt}>
                {formatDateTime(result.note.createdAt)}
              </time>
              {result.note.updatedAt !== result.note.createdAt ? (
                <>
                  <span aria-hidden="true">·</span>
                  <span>수정됨 {formatDateTime(result.note.updatedAt)}</span>
                </>
              ) : null}
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <Link
                href={`/dev-notes/${result.note.id}/edit`}
                className="flex h-9 items-center justify-center rounded-md border border-zinc-300 bg-white px-3 text-xs font-medium text-zinc-900 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-50 dark:hover:bg-zinc-900"
              >
                수정
              </Link>
              <DeleteDevNoteButton noteId={result.note.id} />
            </div>
          </header>

          <div className="whitespace-pre-wrap text-base leading-7 text-zinc-800 dark:text-zinc-200">
            {result.note.content}
          </div>
        </article>
      )}
    </div>
  );
}

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const h = String(d.getHours()).padStart(2, "0");
  const min = String(d.getMinutes()).padStart(2, "0");
  return `${y}-${m}-${day} ${h}:${min}`;
}
