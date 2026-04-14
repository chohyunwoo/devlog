export type PostFormState = {
  fieldErrors: Record<string, string>;
  formError: string | null;
  values: {
    title: string;
    content: string;
    tags: string;
  };
};

export const initialPostFormState: PostFormState = {
  fieldErrors: {},
  formError: null,
  values: { title: "", content: "", tags: "" },
};

export function parseTagsInput(raw: string): string[] {
  const seen = new Set<string>();
  for (const tag of raw.split(",")) {
    const trimmed = tag.trim();
    if (trimmed.length > 0) seen.add(trimmed);
  }
  return [...seen];
}
