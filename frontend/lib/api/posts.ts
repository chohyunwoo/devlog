import "server-only";

import { cache } from "react";

import { apiRequest } from "./http";

export const POSTS_CACHE_TAG = "posts";

export type AuthorSummary = {
  id: number;
  nickname: string;
};

export type PostSummary = {
  id: number;
  title: string;
  author: AuthorSummary;
  tags: string[];
  createdAt: string;
  updatedAt: string;
};

export type Post = PostSummary & {
  content: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type PostCreateRequest = {
  title: string;
  content: string;
  tags: string[];
};

export type ListPostsQuery = {
  page?: number;
  size?: number;
  sort?: string;
  authorId?: number;
  tag?: string;
};

function buildListQuery(query: ListPostsQuery): string {
  const params = new URLSearchParams();
  if (query.page !== undefined) params.set("page", String(query.page));
  if (query.size !== undefined) params.set("size", String(query.size));
  if (query.sort) params.set("sort", query.sort);
  if (query.authorId !== undefined)
    params.set("authorId", String(query.authorId));
  if (query.tag) params.set("tag", query.tag);
  const qs = params.toString();
  return qs ? `?${qs}` : "";
}

export const listPosts = cache(
  (query: ListPostsQuery = {}): Promise<PageResponse<PostSummary>> =>
    apiRequest<PageResponse<PostSummary>>(
      `/api/posts${buildListQuery(query)}`,
      { next: { revalidate: 60, tags: [POSTS_CACHE_TAG] } },
    ),
);

export const getPost = cache(
  (postId: number): Promise<Post> =>
    apiRequest<Post>(`/api/posts/${postId}`, {
      next: { revalidate: 60, tags: [POSTS_CACHE_TAG] },
    }),
);

export function createPost(body: PostCreateRequest): Promise<Post> {
  return apiRequest<Post>("/api/posts", {
    method: "POST",
    body,
    auth: true,
  });
}
