"use client";

import { useEffect, useRef, useState } from "react";
import { useFormStatus } from "react-dom";

import { deleteDevNoteAction } from "@/app/actions/dev-notes";

type DeleteDevNoteButtonProps = {
  noteId: number;
};

function ConfirmSubmit() {
  const { pending } = useFormStatus();
  return (
    <button
      type="submit"
      disabled={pending}
      className="flex h-9 items-center justify-center rounded-md bg-red-600 px-3 text-xs font-medium text-white transition-colors hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
    >
      {pending ? "삭제 중..." : "삭제 확인"}
    </button>
  );
}

export function DeleteDevNoteButton({ noteId }: DeleteDevNoteButtonProps) {
  const [confirming, setConfirming] = useState(false);
  const cancelRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (confirming) cancelRef.current?.focus();
  }, [confirming]);

  if (!confirming) {
    return (
      <button
        type="button"
        onClick={() => setConfirming(true)}
        className="flex h-9 items-center justify-center rounded-md border border-red-300 bg-white px-3 text-xs font-medium text-red-700 transition-colors hover:bg-red-50 dark:border-red-900 dark:bg-zinc-950 dark:text-red-400 dark:hover:bg-red-950/40"
      >
        삭제
      </button>
    );
  }

  return (
    <form
      action={deleteDevNoteAction}
      role="group"
      aria-label="삭제 확인"
      className="flex items-center gap-2"
    >
      <input type="hidden" name="noteId" value={noteId} />
      <span role="status" className="text-xs text-zinc-600 dark:text-zinc-400">
        정말 삭제하시겠습니까?
      </span>
      <button
        ref={cancelRef}
        type="button"
        onClick={() => setConfirming(false)}
        className="flex h-9 items-center justify-center rounded-md border border-zinc-300 bg-white px-3 text-xs font-medium text-zinc-900 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-50 dark:hover:bg-zinc-900"
      >
        취소
      </button>
      <ConfirmSubmit />
    </form>
  );
}
