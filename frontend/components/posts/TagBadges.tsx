import Link from "next/link";

type TagBadgesProps = {
  tags: string[];
  linkToFilter?: boolean;
};

export function TagBadges({ tags, linkToFilter = false }: TagBadgesProps) {
  if (tags.length === 0) return null;

  return (
    <ul className="flex flex-wrap gap-1.5" aria-label="태그">
      {tags.map((tag) => {
        const label = `#${tag}`;
        const className =
          "inline-flex h-6 items-center rounded-full border border-zinc-200 bg-zinc-50 px-2 text-xs font-medium text-zinc-700 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300";
        return (
          <li key={tag}>
            {linkToFilter ? (
              <Link
                href={`/posts?tag=${encodeURIComponent(tag)}`}
                className={`${className} transition-colors hover:border-zinc-400 hover:bg-zinc-100 dark:hover:border-zinc-600 dark:hover:bg-zinc-800`}
              >
                {label}
              </Link>
            ) : (
              <span className={className}>{label}</span>
            )}
          </li>
        );
      })}
    </ul>
  );
}
