"use client";

import Link from "next/link";
import { useActionState } from "react";

import {
  createDevNoteAction,
  updateDevNoteAction,
} from "@/app/actions/dev-notes";
import {
  initialDevNoteFormState,
  type DevNoteFormState,
} from "@/app/actions/dev-note-form-state";

import { FormField } from "@/components/auth/FormField";
import { TextareaField } from "@/components/ui/TextareaField";

type DevNoteFormProps =
  | { mode: "create" }
  | {
      mode: "edit";
      noteId: number;
      initialValues: { title: string; content: string };
    };

export function DevNoteForm(props: DevNoteFormProps) {
  const initialState: DevNoteFormState =
    props.mode === "edit"
      ? {
          fieldErrors: {},
          formError: null,
          values: props.initialValues,
        }
      : initialDevNoteFormState;

  const action =
    props.mode === "edit"
      ? updateDevNoteAction.bind(null, props.noteId)
      : createDevNoteAction;

  const cancelHref =
    props.mode === "edit" ? `/dev-notes/${props.noteId}` : "/dev-notes";
  const submitLabel = props.mode === "edit" ? "수정 완료" : "작성 완료";
  const pendingLabel = props.mode === "edit" ? "수정 중..." : "작성 중...";

  const [state, formAction, pending] = useActionState(action, initialState);

  return (
    <form action={formAction} className="flex flex-col gap-5" noValidate>
      <FormField
        id="title"
        label="제목"
        name="title"
        type="text"
        required
        maxLength={200}
        placeholder="YYYY-MM-DD TIL"
        defaultValue={state.values.title}
        error={state.fieldErrors.title}
      />

      <TextareaField
        id="content"
        label="본문"
        name="content"
        required
        rows={14}
        defaultValue={state.values.content}
        error={state.fieldErrors.content}
      />

      {state.formError ? (
        <div
          role="alert"
          className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/40 dark:text-red-300"
        >
          {state.formError}
        </div>
      ) : null}

      <div className="flex items-center justify-end gap-3">
        <Link
          href={cancelHref}
          className="flex h-11 items-center justify-center rounded-md border border-zinc-300 bg-white px-4 text-sm font-medium text-zinc-900 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-50 dark:hover:bg-zinc-900"
        >
          취소
        </Link>
        <button
          type="submit"
          disabled={pending}
          className="flex h-11 min-w-32 items-center justify-center rounded-md bg-zinc-900 px-4 text-sm font-medium text-zinc-50 transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200"
        >
          {pending ? pendingLabel : submitLabel}
        </button>
      </div>
    </form>
  );
}
