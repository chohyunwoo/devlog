"use server";

import { redirect } from "next/navigation";

import {
  loginRequest,
  signupRequest,
  type LoginRequest,
  type SignupRequest,
} from "@/lib/api/auth";
import { ApiError, NetworkError } from "@/lib/api/errors";
import { createSession, destroySession } from "@/lib/session";

import type { AuthFormState } from "./auth-form-state";

function toFieldErrors(error: ApiError): Record<string, string> {
  const out: Record<string, string> = {};
  for (const fe of error.fieldErrors) {
    if (!out[fe.field]) out[fe.field] = fe.message;
  }
  return out;
}

function failure(
  values: Record<string, string>,
  error: unknown,
  defaultMessage: string,
): AuthFormState {
  if (error instanceof ApiError) {
    return {
      fieldErrors: toFieldErrors(error),
      formError:
        error.fieldErrors.length > 0 ? null : error.message || defaultMessage,
      values,
    };
  }
  if (error instanceof NetworkError) {
    return { fieldErrors: {}, formError: error.message, values };
  }
  return { fieldErrors: {}, formError: defaultMessage, values };
}

export async function signupAction(
  _prev: AuthFormState,
  formData: FormData,
): Promise<AuthFormState> {
  const email = (formData.get("email") ?? "").toString().trim();
  const password = (formData.get("password") ?? "").toString();
  const nickname = (formData.get("nickname") ?? "").toString().trim();
  const values = { email, nickname };

  const payload: SignupRequest = { email, password, nickname };

  try {
    await signupRequest(payload);
  } catch (error) {
    return failure(values, error, "회원가입에 실패했습니다.");
  }

  try {
    const tokens = await loginRequest({ email, password });
    await createSession(tokens);
  } catch {
    redirect("/login?signup=success");
  }

  redirect("/");
}

export async function loginAction(
  _prev: AuthFormState,
  formData: FormData,
): Promise<AuthFormState> {
  const email = (formData.get("email") ?? "").toString().trim();
  const password = (formData.get("password") ?? "").toString();
  const values = { email };

  const payload: LoginRequest = { email, password };

  let tokens;
  try {
    tokens = await loginRequest(payload);
  } catch (error) {
    return failure(values, error, "로그인에 실패했습니다.");
  }

  await createSession(tokens);
  redirect("/");
}

export async function logoutAction(): Promise<void> {
  await destroySession();
  redirect("/login");
}
