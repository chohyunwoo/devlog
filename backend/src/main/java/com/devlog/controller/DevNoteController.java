package com.devlog.controller;

import com.devlog.controller.dto.DevNoteCreateRequest;
import com.devlog.controller.dto.DevNoteResponse;
import com.devlog.controller.dto.DevNoteSummaryResponse;
import com.devlog.controller.dto.DevNoteUpdateRequest;
import com.devlog.controller.dto.PageResponse;
import com.devlog.security.AuthenticatedUser;
import com.devlog.service.DevNoteService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev-notes")
public class DevNoteController {

    private final DevNoteService devNoteService;

    public DevNoteController(DevNoteService devNoteService) {
        this.devNoteService = devNoteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DevNoteResponse create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody DevNoteCreateRequest request
    ) {
        return DevNoteResponse.from(devNoteService.create(principal.userId(), request));
    }

    @GetMapping
    public PageResponse<DevNoteSummaryResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return PageResponse.from(
                devNoteService.findByAuthor(principal.userId(), pageable)
                        .map(DevNoteSummaryResponse::from));
    }

    @GetMapping("/{noteId}")
    public DevNoteResponse detail(
            @PathVariable Long noteId,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return DevNoteResponse.from(devNoteService.findDetail(noteId, principal.userId()));
    }

    @PutMapping("/{noteId}")
    public DevNoteResponse update(
            @PathVariable Long noteId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody DevNoteUpdateRequest request
    ) {
        return DevNoteResponse.from(
                devNoteService.update(noteId, principal.userId(), request));
    }

    @DeleteMapping("/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long noteId,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        devNoteService.delete(noteId, principal.userId());
    }
}
