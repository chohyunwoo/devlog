import type { InputHTMLAttributes, ReactNode } from "react";

import {
  fieldAriaDescribedBy,
  FieldShell,
} from "@/components/ui/FieldShell";
import { cn } from "@/lib/utils";

type FormFieldProps = {
  id: string;
  label: string;
  error?: string;
  hint?: ReactNode;
} & InputHTMLAttributes<HTMLInputElement>;

export function FormField({
  id,
  label,
  error,
  hint,
  className,
  ...inputProps
}: FormFieldProps) {
  return (
    <FieldShell id={id} label={label} error={error} hint={hint}>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={fieldAriaDescribedBy(id, error, hint)}
        className={cn(
          "h-11 rounded-md border bg-white px-3 text-sm text-zinc-900 outline-none transition-colors placeholder:text-zinc-400 focus-visible:ring-2 focus-visible:ring-zinc-900/10 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-zinc-950 dark:text-zinc-50 dark:placeholder:text-zinc-500 dark:focus-visible:ring-zinc-50/10",
          error
            ? "border-red-500 focus:border-red-500 dark:border-red-500"
            : "border-zinc-300 focus:border-zinc-900 dark:border-zinc-700 dark:focus:border-zinc-50",
          className,
        )}
        {...inputProps}
      />
    </FieldShell>
  );
}
