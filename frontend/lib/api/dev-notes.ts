import "server-only";

import { cache } from "react";

import { apiRequest } from "./http";

export type DevNoteSummary = {
  id: number;
  title: string;
  createdAt: string;
  updatedAt: string;
};

export type DevNote = DevNoteSummary & {
  content: string;
};

export type DevNotePageResponse = {
  content: DevNoteSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type DevNoteCreateRequest = {
  title: string;
  content: string;
};

export type DevNoteUpdateRequest = DevNoteCreateRequest;

export type ListDevNotesQuery = {
  page?: number;
  size?: number;
  sort?: string;
};

function buildListQuery(query: ListDevNotesQuery): string {
  const params = new URLSearchParams();
  if (query.page !== undefined) params.set("page", String(query.page));
  if (query.size !== undefined) params.set("size", String(query.size));
  if (query.sort) params.set("sort", query.sort);
  const qs = params.toString();
  return qs ? `?${qs}` : "";
}

export const listDevNotes = cache(
  (query: ListDevNotesQuery = {}): Promise<DevNotePageResponse> =>
    apiRequest<DevNotePageResponse>(
      `/api/dev-notes${buildListQuery(query)}`,
      { auth: true },
    ),
);

export const getDevNote = cache(
  (noteId: number): Promise<DevNote> =>
    apiRequest<DevNote>(`/api/dev-notes/${noteId}`, { auth: true }),
);

export function createDevNote(body: DevNoteCreateRequest): Promise<DevNote> {
  return apiRequest<DevNote>("/api/dev-notes", {
    method: "POST",
    body,
    auth: true,
  });
}

export function updateDevNote(
  noteId: number,
  body: DevNoteUpdateRequest,
): Promise<DevNote> {
  return apiRequest<DevNote>(`/api/dev-notes/${noteId}`, {
    method: "PUT",
    body,
    auth: true,
  });
}

export function deleteDevNote(noteId: number): Promise<void> {
  return apiRequest<void>(`/api/dev-notes/${noteId}`, {
    method: "DELETE",
    auth: true,
  });
}
