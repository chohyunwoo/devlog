import Link from "next/link";

import type { PostSummary } from "@/lib/api/posts";

import { TagBadges } from "./TagBadges";

type PostCardProps = {
  post: PostSummary;
};

export function PostCard({ post }: PostCardProps) {
  return (
    <article className="group relative rounded-lg border border-zinc-200 bg-white p-5 transition-colors focus-within:border-zinc-500 hover:border-zinc-300 dark:border-zinc-800 dark:bg-zinc-950 dark:focus-within:border-zinc-400 dark:hover:border-zinc-700">
      <header className="flex flex-col gap-1">
        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-50">
          <Link
            href={`/posts/${post.id}`}
            className="outline-none after:absolute after:inset-0 after:rounded-lg focus-visible:after:ring-2 focus-visible:after:ring-zinc-900/20 dark:focus-visible:after:ring-zinc-50/20"
          >
            {post.title}
          </Link>
        </h2>
        <div className="flex items-center gap-2 text-xs text-zinc-500 dark:text-zinc-400">
          <span>{post.author.nickname}</span>
          <span aria-hidden="true">·</span>
          <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
        </div>
      </header>
      {post.tags.length > 0 ? (
        <div className="relative z-10 mt-3">
          <TagBadges tags={post.tags} linkToFilter />
        </div>
      ) : null}
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
