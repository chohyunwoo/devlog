package com.devlog.controller;

import com.devlog.controller.dto.PageResponse;
import com.devlog.controller.dto.PostCreateRequest;
import com.devlog.controller.dto.PostResponse;
import com.devlog.controller.dto.PostSummaryResponse;
import com.devlog.controller.dto.PostUpdateRequest;
import com.devlog.domain.Post;
import com.devlog.security.AuthenticatedUser;
import com.devlog.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PostCreateRequest request
    ) {
        Post post = postService.create(principal.userId(), request);
        return PostResponse.from(post);
    }

    @GetMapping
    public PageResponse<PostSummaryResponse> list(
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String tag,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        if (authorId != null && tag != null) {
            throw new IllegalArgumentException("authorId와 tag 는 동시에 지정할 수 없습니다.");
        }
        Page<Post> page;
        if (authorId != null) {
            page = postService.findByAuthor(authorId, pageable);
        } else if (tag != null) {
            page = postService.findByTag(tag, pageable);
        } else {
            page = postService.findAll(pageable);
        }
        return PageResponse.from(page.map(PostSummaryResponse::from));
    }

    @GetMapping("/{postId}")
    public PostResponse detail(@PathVariable Long postId) {
        Post post = postService.findDetail(postId);
        return PostResponse.from(post);
    }

    @PutMapping("/{postId}")
    public PostResponse update(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        Post post = postService.update(postId, principal.userId(), request);
        return PostResponse.from(post);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        postService.delete(postId, principal.userId());
    }
}
