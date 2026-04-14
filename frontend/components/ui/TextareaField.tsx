import type { ReactNode, TextareaHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

import { fieldAriaDescribedBy, FieldShell } from "./FieldShell";

type TextareaFieldProps = {
  id: string;
  label: string;
  error?: string;
  hint?: ReactNode;
} & TextareaHTMLAttributes<HTMLTextAreaElement>;

export function TextareaField({
  id,
  label,
  error,
  hint,
  className,
  rows = 10,
  ...textareaProps
}: TextareaFieldProps) {
  return (
    <FieldShell id={id} label={label} error={error} hint={hint}>
      <textarea
        id={id}
        rows={rows}
        aria-invalid={error ? true : undefined}
        aria-describedby={fieldAriaDescribedBy(id, error, hint)}
        className={cn(
          "min-h-48 rounded-md border bg-white px-3 py-2 text-sm text-zinc-900 outline-none transition-colors placeholder:text-zinc-400 focus-visible:ring-2 focus-visible:ring-zinc-900/10 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-zinc-950 dark:text-zinc-50 dark:placeholder:text-zinc-500 dark:focus-visible:ring-zinc-50/10",
          error
            ? "border-red-500 focus:border-red-500 dark:border-red-500"
            : "border-zinc-300 focus:border-zinc-900 dark:border-zinc-700 dark:focus:border-zinc-50",
          className,
        )}
        {...textareaProps}
      />
    </FieldShell>
  );
}
