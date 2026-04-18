import type { Metadata } from "next";
import { notFound, redirect } from "next/navigation";

import { DevNoteForm } from "@/components/dev-notes/DevNoteForm";
import { ApiError } from "@/lib/api/errors";
import { getDevNote, type DevNote } from "@/lib/api/dev-notes";
import { hasAccessToken } from "@/lib/session";

export const metadata: Metadata = {
  title: "개발 일기 수정 | DevLog",
};

type EditDevNotePageProps = {
  params: Promise<{ id: string }>;
};

export default async function EditDevNotePage({ params }: EditDevNotePageProps) {
  const { id } = await params;

  if (!(await hasAccessToken())) {
    redirect(`/login?next=/dev-notes/${id}/edit`);
  }

  const noteId = Number.parseInt(id, 10);
  if (!Number.isFinite(noteId) || noteId <= 0) {
    notFound();
  }

  let note: DevNote;
  try {
    note = await getDevNote(noteId);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    if (error instanceof ApiError && error.status === 401) {
      redirect(`/login?next=/dev-notes/${id}/edit`);
    }
    throw error;
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-6 py-10">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50">
          개발 일기 수정
        </h1>
      </header>
      <DevNoteForm
        mode="edit"
        noteId={note.id}
        initialValues={{ title: note.title, content: note.content }}
      />
    </div>
  );
}
