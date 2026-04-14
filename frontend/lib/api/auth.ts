import "server-only";

import { apiRequest } from "./http";

export type SignupRequest = {
  email: string;
  password: string;
  nickname: string;
};

export type SignupResponse = {
  id: number;
  email: string;
  nickname: string;
  createdAt: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
};

export function signupRequest(body: SignupRequest): Promise<SignupResponse> {
  return apiRequest<SignupResponse>("/api/auth/signup", {
    method: "POST",
    body,
  });
}

export function loginRequest(body: LoginRequest): Promise<LoginResponse> {
  return apiRequest<LoginResponse>("/api/auth/login", {
    method: "POST",
    body,
  });
}
