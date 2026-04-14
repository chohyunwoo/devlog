import "server-only";

import { cookies } from "next/headers";

import { ACCESS_TOKEN_COOKIE } from "@/lib/session";

import {
  ApiError,
  type BackendErrorBody,
  NetworkError,
} from "./errors";

type Method = "GET" | "POST" | "PUT" | "DELETE";

type RequestOptions = {
  method?: Method;
  body?: unknown;
  auth?: boolean;
  cache?: RequestCache;
  next?: { revalidate?: number | false; tags?: string[] };
};

function backendBaseUrl(): string {
  const url = process.env.BACKEND_API_URL;
  if (!url) {
    throw new Error("BACKEND_API_URL 환경변수가 설정되지 않았습니다.");
  }
  return url.replace(/\/$/, "");
}

export async function apiRequest<T>(
  path: string,
  {
    method = "GET",
    body,
    auth = false,
    cache,
    next,
  }: RequestOptions = {},
): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  if (auth) {
    const accessToken = (await cookies()).get(ACCESS_TOKEN_COOKIE)?.value;
    if (accessToken) {
      headers["Authorization"] = `Bearer ${accessToken}`;
    }
  }

  const fetchInit: RequestInit & {
    next?: { revalidate?: number | false; tags?: string[] };
  } = {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  };

  if (next) {
    fetchInit.next = next;
  } else {
    fetchInit.cache = cache ?? "no-store";
  }

  let response: Response;
  try {
    response = await fetch(`${backendBaseUrl()}${path}`, fetchInit);
  } catch (cause) {
    throw new NetworkError(cause);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const parsed = text ? safeParseJson(text) : undefined;

  if (!response.ok) {
    const errorBody: BackendErrorBody = isBackendErrorBody(parsed)
      ? parsed
      : {
          code: "UNKNOWN_ERROR",
          message: response.statusText || "요청을 처리하지 못했습니다.",
          fieldErrors: [],
        };
    throw new ApiError(response.status, errorBody);
  }

  return parsed as T;
}

function safeParseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

function isBackendErrorBody(value: unknown): value is BackendErrorBody {
  if (!value || typeof value !== "object") return false;
  const v = value as Record<string, unknown>;
  return typeof v.code === "string" && typeof v.message === "string";
}
