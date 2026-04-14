import type { ReactNode } from "react";

type FieldShellProps = {
  id: string;
  label: string;
  error?: string;
  hint?: ReactNode;
  children: ReactNode;
};

export function FieldShell({
  id,
  label,
  error,
  hint,
  children,
}: FieldShellProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label
        htmlFor={id}
        className="text-sm font-medium text-zinc-800 dark:text-zinc-200"
      >
        {label}
      </label>
      {children}
      {error ? (
        <p
          id={`${id}-error`}
          className="text-xs text-red-600 dark:text-red-400"
        >
          {error}
        </p>
      ) : hint ? (
        <p
          id={`${id}-hint`}
          className="text-xs text-zinc-500 dark:text-zinc-400"
        >
          {hint}
        </p>
      ) : null}
    </div>
  );
}

export function fieldAriaDescribedBy(
  id: string,
  error: string | undefined,
  hint: ReactNode | undefined,
): string | undefined {
  if (error) return `${id}-error`;
  if (hint) return `${id}-hint`;
  return undefined;
}
