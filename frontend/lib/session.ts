import "server-only";

import { cookies } from "next/headers";

export const ACCESS_TOKEN_COOKIE = "devlog_access_token";
export const REFRESH_TOKEN_COOKIE = "devlog_refresh_token";

const ACCESS_TOKEN_MAX_AGE_SECONDS = 60 * 60;
const REFRESH_TOKEN_MAX_AGE_SECONDS = 60 * 60 * 24 * 14;

type SessionTokens = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
};

export async function createSession({
  accessToken,
  refreshToken,
  expiresIn,
}: SessionTokens): Promise<void> {
  const cookieStore = await cookies();
  const isProd = process.env.NODE_ENV === "production";

  cookieStore.set(ACCESS_TOKEN_COOKIE, accessToken, {
    httpOnly: true,
    secure: isProd,
    sameSite: "lax",
    path: "/",
    maxAge: expiresIn > 0 ? expiresIn : ACCESS_TOKEN_MAX_AGE_SECONDS,
  });

  cookieStore.set(REFRESH_TOKEN_COOKIE, refreshToken, {
    httpOnly: true,
    secure: isProd,
    sameSite: "lax",
    path: "/",
    maxAge: REFRESH_TOKEN_MAX_AGE_SECONDS,
  });
}

export async function destroySession(): Promise<void> {
  const cookieStore = await cookies();
  cookieStore.delete(ACCESS_TOKEN_COOKIE);
  cookieStore.delete(REFRESH_TOKEN_COOKIE);
}

export async function hasAccessToken(): Promise<boolean> {
  const cookieStore = await cookies();
  return cookieStore.has(ACCESS_TOKEN_COOKIE);
}
