import Link from "next/link";

type PaginationProps = {
  page: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  buildHref: (page: number) => string;
};

export function Pagination({
  page,
  totalPages,
  first,
  last,
  buildHref,
}: PaginationProps) {
  if (totalPages <= 1) return null;

  const baseClass =
    "flex h-10 min-w-24 items-center justify-center rounded-md border border-zinc-300 bg-white px-4 text-sm font-medium text-zinc-900 transition-colors hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-50 dark:hover:bg-zinc-900";
  const disabledClass =
    "flex h-10 min-w-24 items-center justify-center rounded-md border border-zinc-200 bg-zinc-50 px-4 text-sm font-medium text-zinc-400 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-600";

  return (
    <nav
      aria-label="페이지네이션"
      className="flex items-center justify-between gap-4"
    >
      {first ? (
        <span className={disabledClass} aria-disabled="true">
          이전
        </span>
      ) : (
        <Link href={buildHref(page - 1)} className={baseClass} rel="prev">
          이전
        </Link>
      )}
      <span className="text-sm text-zinc-600 dark:text-zinc-400">
        {page + 1} / {totalPages}
      </span>
      {last ? (
        <span className={disabledClass} aria-disabled="true">
          다음
        </span>
      ) : (
        <Link href={buildHref(page + 1)} className={baseClass} rel="next">
          다음
        </Link>
      )}
    </nav>
  );
}
