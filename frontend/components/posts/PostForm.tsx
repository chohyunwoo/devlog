"use client";

import Link from "next/link";
import { useActionState } from "react";

import { createPostAction } from "@/app/actions/posts";
import { initialPostFormState } from "@/app/actions/post-form-state";

import { FormField } from "@/components/auth/FormField";
import { TextareaField } from "@/components/ui/TextareaField";

export function PostForm() {
  const [state, action, pending] = useActionState(
    createPostAction,
    initialPostFormState,
  );

  return (
    <form action={action} className="flex flex-col gap-5" noValidate>
      <FormField
        id="title"
        label="제목"
        name="title"
        type="text"
        required
        maxLength={200}
        placeholder="포스트 제목"
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

      <FormField
        id="tags"
        label="태그"
        name="tags"
        type="text"
        maxLength={500}
        placeholder="java, spring, boot"
        defaultValue={state.values.tags}
        hint="쉼표로 구분해 입력하세요. 비워두셔도 됩니다."
        error={state.fieldErrors.tags}
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
          href="/posts"
          className="flex h-11 items-center justify-center rounded-md border border-zinc-300 bg-white px-4 text-sm font-medium text-zinc-900 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-50 dark:hover:bg-zinc-900"
        >
          취소
        </Link>
        <button
          type="submit"
          disabled={pending}
          className="flex h-11 min-w-32 items-center justify-center rounded-md bg-zinc-900 px-4 text-sm font-medium text-zinc-50 transition-colors hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200"
        >
          {pending ? "작성 중..." : "작성 완료"}
        </button>
      </div>
    </form>
  );
}
