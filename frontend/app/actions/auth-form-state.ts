export type AuthFormState = {
  fieldErrors: Record<string, string>;
  formError: string | null;
  values: Record<string, string>;
};

export const initialAuthFormState: AuthFormState = {
  fieldErrors: {},
  formError: null,
  values: {},
};
