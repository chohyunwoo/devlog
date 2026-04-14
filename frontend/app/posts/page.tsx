import type { Metadata } from "next";
import Link from "next/link";

import { redirect } from "next/navigation";

import { PostCard } from "@/components/posts/PostCard";
import { Pagination } from "@/components/posts/Pagination";
import { TagBadges } from "@/components/posts/TagBadges";
import { toErrorMessage } from "@/lib/api/errors";
import { listPosts, type PageResponse, type PostSummary } from "@/lib/api/posts";
import { hasAccessToken } from "@/lib/session";

export const metadata: Metadata = {
  title: "포스트 | DevLog",
};

type PostsPageProps = {
  searchParams: Promise<{
    page?: string;
    tag?: string;
    authorId?: string;
  }>;
};

const DEFAULT_PAGE_SIZE = 20;

export default async function PostsPage({ searchParams }: PostsPageProps) {
  const params = await searchParams;
  const page = parsePageNumber(params.page);
  const tag = params.tag?.trim() || undefined;
  // tag 와 authorId 동시 지정 시 tag 우선 (백엔드는 400 을 던짐).
  const authorId = tag ? undefined : parseAuthorId(params.authorId);
  const authed = await hasAccessToken();

  let result: PageResponse<PostSummary> | null = null;
  let errorMessage: string | null = null;

  try {
    result = await listPosts({
      page,
      size: DEFAULT_PAGE_SIZE,
      tag,
      authorId,
    });
  } catch (error) {
    errorMessage = toErrorMessage(error, "포스트 목록을 불러오지 못했습니다.");
  }

  if (
    result &&
    result.totalPages > 0 &&
    page >= result.totalPages
  ) {
    redirect(
      buildPostsHref({ page: result.totalPages - 1, tag, authorId }),
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-8 px-6 py-10">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50">
            포스트
          </h1>
          <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
            공개 블로그 영역 · 누구나 읽을 수 있습니다.
          </p>
        </div>
        {authed ? (
          <Link
            href="/posts/new"
            className="flex h-10 items-center justify-center rounded-md bg-zinc-900 px-4 text-sm font-medium text-zinc-50 transition-colors hover:bg-zinc-800 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200"
          >
            새 포스트 작성
          </Link>
        ) : null}
      </header>

      {tag ? (
        <div className="flex items-center gap-2 text-sm text-zinc-600 dark:text-zinc-400">
          <span>태그 필터:</span>
          <TagBadges tags={[tag]} />
          <Link
            href="/posts"
            className="ml-auto text-xs font-medium underline-offset-4 hover:underline"
          >
            필터 해제
          </Link>
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
          아직 등록된 포스트가 없습니다.
        </p>
      ) : result ? (
        <>
          <ul className="flex flex-col gap-4">
            {result.content.map((post) => (
              <li key={post.id}>
                <PostCard post={post} />
              </li>
            ))}
          </ul>
          <Pagination
            page={result.page}
            totalPages={result.totalPages}
            first={result.first}
            last={result.last}
            buildHref={(p) => buildPostsHref({ page: p, tag, authorId })}
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

function parseAuthorId(raw: string | undefined): number | undefined {
  if (!raw) return undefined;
  const n = Number.parseInt(raw, 10);
  return Number.isFinite(n) && n > 0 ? n : undefined;
}

function buildPostsHref(input: {
  page: number;
  tag?: string;
  authorId?: number;
}): string {
  const params = new URLSearchParams();
  if (input.page > 0) params.set("page", String(input.page));
  if (input.tag) params.set("tag", input.tag);
  if (input.authorId !== undefined)
    params.set("authorId", String(input.authorId));
  const qs = params.toString();
  return qs ? `/posts?${qs}` : "/posts";
}

