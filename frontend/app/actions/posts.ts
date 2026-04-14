"use server";

import { updateTag } from "next/cache";
import { redirect } from "next/navigation";

import { createPost, POSTS_CACHE_TAG } from "@/lib/api/posts";
import { ApiError, NetworkError } from "@/lib/api/errors";

import {
  parseTagsInput,
  type PostFormState,
} from "./post-form-state";

function toFieldErrors(error: ApiError): Record<string, string> {
  const out: Record<string, string> = {};
  for (const fe of error.fieldErrors) {
    if (!out[fe.field]) out[fe.field] = fe.message;
  }
  return out;
}

function failure(
  values: PostFormState["values"],
  error: unknown,
  defaultMessage: string,
): PostFormState {
  if (error instanceof ApiError) {
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

export async function createPostAction(
  _prev: PostFormState,
  formData: FormData,
): Promise<PostFormState> {
  const title = (formData.get("title") ?? "").toString().trim();
  const content = (formData.get("content") ?? "").toString();
  const tagsRaw = (formData.get("tags") ?? "").toString();
  const values = { title, content, tags: tagsRaw };

  let created;
  try {
    created = await createPost({
      title,
      content,
      tags: parseTagsInput(tagsRaw),
    });
  } catch (error) {
    return failure(values, error, "포스트 작성에 실패했습니다.");
  }

  updateTag(POSTS_CACHE_TAG);
  redirect(`/posts/${created.id}`);
}
