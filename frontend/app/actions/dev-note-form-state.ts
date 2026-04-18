export type DevNoteFormState = {
  fieldErrors: Record<string, string>;
  formError: string | null;
  values: {
    title: string;
    content: string;
  };
};

export const initialDevNoteFormState: DevNoteFormState = {
  fieldErrors: {},
  formError: null,
  values: { title: "", content: "" },
};
