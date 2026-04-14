import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { TagBadges } from "@/components/posts/TagBadges";
import { ApiError, toErrorMessage } from "@/lib/api/errors";
import { getPost, type Post } from "@/lib/api/posts";

type PostDetailPageProps = {
  params: Promise<{ id: string }>;
};

type LoadResult =
  | { status: "ok"; post: Post }
  | { status: "not_found" }
  | { status: "error"; message: string };

async function loadPost(
  params: PostDetailPageProps["params"],
): Promise<LoadResult> {
  const { id } = await params;
  const postId = Number.parseInt(id, 10);
  if (!Number.isFinite(postId) || postId <= 0) {
    return { status: "not_found" };
  }

  try {
    const post = await getPost(postId);
    return { status: "ok", post };
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return { status: "not_found" };
    }
    return { status: "error", message: toErrorMessage(error, "포스트를 불러오지 못했습니다.") };
  }
}

export async function generateMetadata({
  params,
}: PostDetailPageProps): Promise<Metadata> {
  const result = await loadPost(params);
  if (result.status === "ok") {
    return { title: `${result.post.title} | DevLog` };
  }
  return { title: "포스트 | DevLog" };
}

export default async function PostDetailPage({
  params,
}: PostDetailPageProps) {
  const result = await loadPost(params);

  if (result.status === "not_found") {
    notFound();
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-6 py-10">
      <div className="text-sm">
        <Link
          href="/posts"
          className="text-zinc-500 underline-offset-4 hover:underline dark:text-zinc-400"
        >
          ← 목록으로
        </Link>
      </div>

      {result.status === "error" ? (
        <div
          role="alert"
          className="rounded-md border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/40 dark:text-red-300"
        >
          {result.message}
        </div>
      ) : (
        <article className="flex flex-col gap-6">
          <header className="flex flex-col gap-3 border-b border-zinc-200 pb-6 dark:border-zinc-800">
            <h1 className="text-3xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50">
              {result.post.title}
            </h1>
            <div className="flex flex-wrap items-center gap-2 text-sm text-zinc-500 dark:text-zinc-400">
              <span>{result.post.author.nickname}</span>
              <span aria-hidden="true">·</span>
              <time dateTime={result.post.createdAt}>
                {formatDateTime(result.post.createdAt)}
              </time>
              {result.post.updatedAt !== result.post.createdAt ? (
                <>
                  <span aria-hidden="true">·</span>
                  <span>수정됨 {formatDateTime(result.post.updatedAt)}</span>
                </>
              ) : null}
            </div>
            {result.post.tags.length > 0 ? (
              <TagBadges tags={result.post.tags} linkToFilter />
            ) : null}
          </header>

          <div className="whitespace-pre-wrap text-base leading-7 text-zinc-800 dark:text-zinc-200">
            {result.post.content}
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
