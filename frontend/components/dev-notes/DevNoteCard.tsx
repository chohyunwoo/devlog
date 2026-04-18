import Link from "next/link";

import type { DevNoteSummary } from "@/lib/api/dev-notes";

type DevNoteCardProps = {
  note: DevNoteSummary;
};

export function DevNoteCard({ note }: DevNoteCardProps) {
  return (
    <article className="group relative rounded-lg border border-zinc-200 bg-white p-5 transition-colors focus-within:border-zinc-500 hover:border-zinc-300 dark:border-zinc-800 dark:bg-zinc-950 dark:focus-within:border-zinc-400 dark:hover:border-zinc-700">
      <header className="flex flex-col gap-1">
        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-50">
          <Link
            href={`/dev-notes/${note.id}`}
            className="outline-none after:absolute after:inset-0 after:rounded-lg focus-visible:after:ring-2 focus-visible:after:ring-zinc-900/20 dark:focus-visible:after:ring-zinc-50/20"
          >
            {note.title}
          </Link>
        </h2>
        <div className="flex items-center gap-2 text-xs text-zinc-500 dark:text-zinc-400">
          <time dateTime={note.createdAt}>{formatDate(note.createdAt)}</time>
          {note.updatedAt !== note.createdAt ? (
            <>
              <span aria-hidden="true">·</span>
              <span>수정 {formatDate(note.updatedAt)}</span>
            </>
          ) : null}
        </div>
      </header>
    </article>
  );
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}
