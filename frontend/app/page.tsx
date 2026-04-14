import Link from "next/link";

import { PostCard } from "@/components/posts/PostCard";
import { toErrorMessage } from "@/lib/api/errors";
import { listPosts, type PageResponse, type PostSummary } from "@/lib/api/posts";
import { hasAccessToken } from "@/lib/session";

const HOME_POST_COUNT = 5;

export default async function Home() {
  const authed = await hasAccessToken();

  let recent: PageResponse<PostSummary> | null = null;
  let recentError: string | null = null;
  try {
    recent = await listPosts({ page: 0, size: HOME_POST_COUNT });
  } catch (error) {
    recentError = toErrorMessage(error, "최근 포스트를 불러오지 못했습니다.");
  }

  return (
    <div className="flex flex-1 flex-col bg-zinc-50 dark:bg-black">
      <section className="mx-auto flex w-full max-w-3xl flex-col items-center gap-8 px-6 py-16 text-center">
        <div className="flex flex-col gap-3">
          <span className="text-xs font-medium uppercase tracking-widest text-zinc-500 dark:text-zinc-400">
            Personal Dev Blog
          </span>
          <h1 className="text-4xl font-semibold tracking-tight text-zinc-900 dark:text-zinc-50 sm:text-5xl">
            DevLog
          </h1>
          <p className="text-base text-zinc-600 dark:text-zinc-400 sm:text-lg">
            개발 일기와 미니 블로그를 한 곳에. 공개 포스트로 기록하고, 비공개
            노트로 회고하세요.
          </p>
        </div>

        <div className="flex flex-col items-center gap-3 sm:flex-row">
          <Link
            href="/posts"
            className="flex h-11 w-48 items-center justify-center rounded-md bg-zinc-900 text-sm font-medium text-zinc-50 transition-colors hover:bg-zinc-800 dark:bg-zinc-50 dark:text-zinc-900 dark:hover:bg-zinc-200"
          >
            포스트 둘러보기
          </Link>
          {authed ? (
            <Link
              href="/posts/new"
              className="flex h-11 w-48 items-center justify-center rounded-md border border-zinc-300 bg-white text-sm font-medium text-zinc-900 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-50 dark:hover:bg-zinc-900"
            >
              새 포스트 작성
            </Link>
          ) : (
            <Link
              href="/signup"
              className="flex h-11 w-48 items-center justify-center rounded-md border border-zinc-300 bg-white text-sm font-medium text-zinc-900 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-50 dark:hover:bg-zinc-900"
            >
              회원가입
            </Link>
          )}
        </div>
      </section>

      <section
        aria-labelledby="recent-posts-heading"
        className="mx-auto w-full max-w-3xl px-6 pb-16"
      >
        <div className="mb-4 flex items-end justify-between gap-4">
          <h2
            id="recent-posts-heading"
            className="text-xl font-semibold text-zinc-900 dark:text-zinc-50"
          >
            최근 포스트
          </h2>
          {recent && recent.totalElements > HOME_POST_COUNT ? (
            <Link
              href="/posts"
              className="text-sm font-medium text-zinc-600 underline-offset-4 hover:underline dark:text-zinc-400"
            >
              전체 보기 →
            </Link>
          ) : null}
        </div>

        {recentError ? (
          <div
            role="alert"
            className="rounded-md border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/40 dark:text-red-300"
          >
            {recentError}
          </div>
        ) : recent && recent.content.length > 0 ? (
          <ul className="flex flex-col gap-4">
            {recent.content.map((post) => (
              <li key={post.id}>
                <PostCard post={post} />
              </li>
            ))}
          </ul>
        ) : (
          <p className="rounded-md border border-dashed border-zinc-300 bg-zinc-50 px-4 py-12 text-center text-sm text-zinc-500 dark:border-zinc-800 dark:bg-zinc-950 dark:text-zinc-400">
            아직 등록된 포스트가 없습니다. 첫 글을 작성해보세요.
          </p>
        )}
      </section>
    </div>
  );
}
