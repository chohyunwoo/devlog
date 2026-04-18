"use server";

import { redirect } from "next/navigation";

import {
  createDevNote,
  deleteDevNote,
  updateDevNote,
} from "@/lib/api/dev-notes";
import { ApiError, NetworkError } from "@/lib/api/errors";

import type { DevNoteFormState } from "./dev-note-form-state";

function toFieldErrors(error: ApiError): Record<string, string> {
  const out: Record<string, string> = {};
  for (const fe of error.fieldErrors) {
    if (!out[fe.field]) out[fe.field] = fe.message;
  }
  return out;
}

function loginRedirect(next: string): never {
  redirect(`/login?next=${encodeURIComponent(next)}`);
}

function failure(
  values: DevNoteFormState["values"],
  error: unknown,
  defaultMessage: string,
  loginNext: string,
): DevNoteFormState {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      loginRedirect(loginNext);
    }
    return {
      fieldErrors: toFieldErrors(error),
      formError:
        error.fieldErrors.length > 0 ? null : error.message || defaultMessage,
      values,
    };
  }
  if (error instanceof NetworkError) {
    return { fieldErrors: {}, formError: error.message, values };
  }
  return { fieldErrors: {}, formError: defaultMessage, values };
}

export async function createDevNoteAction(
  _prev: DevNoteFormState,
  formData: FormData,
): Promise<DevNoteFormState> {
  const title = (formData.get("title") ?? "").toString().trim();
  const content = (formData.get("content") ?? "").toString();
  const values = { title, content };

  let created;
  try {
    created = await createDevNote({ title, content });
  } catch (error) {
    return failure(
      values,
      error,
      "개발 일기 작성에 실패했습니다.",
      "/dev-notes/new",
    );
  }

  redirect(`/dev-notes/${created.id}`);
}

export async function updateDevNoteAction(
  noteId: number,
  _prev: DevNoteFormState,
  formData: FormData,
): Promise<DevNoteFormState> {
  const title = (formData.get("title") ?? "").toString().trim();
  const content = (formData.get("content") ?? "").toString();
  const values = { title, content };

  try {
    await updateDevNote(noteId, { title, content });
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      redirect("/dev-notes?error=not_found");
    }
    return failure(
      values,
      error,
      "개발 일기 수정에 실패했습니다.",
      `/dev-notes/${noteId}/edit`,
    );
  }

  redirect(`/dev-notes/${noteId}`);
}

export async function deleteDevNoteAction(formData: FormData): Promise<void> {
  const raw = (formData.get("noteId") ?? "").toString();
  const noteId = Number.parseInt(raw, 10);
  if (!Number.isFinite(noteId) || noteId <= 0) {
    redirect("/dev-notes");
  }

  let outcome: "ok" | "not_found" | "failed" = "ok";
  try {
    await deleteDevNote(noteId);
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      loginRedirect(`/dev-notes/${noteId}`);
    }
    outcome =
      error instanceof ApiError && error.status === 404
        ? "not_found"
        : "failed";
  }

  if (outcome === "not_found") redirect("/dev-notes?error=not_found");
  if (outcome === "failed") {
    redirect(`/dev-notes/${noteId}?error=delete_failed`);
  }
  redirect("/dev-notes");
}
