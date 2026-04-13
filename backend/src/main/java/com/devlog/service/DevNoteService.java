package com.devlog.service;

import com.devlog.common.exception.DevNoteNotFoundException;
import com.devlog.controller.dto.DevNoteCreateRequest;
import com.devlog.controller.dto.DevNoteUpdateRequest;
import com.devlog.domain.DevNote;
import com.devlog.domain.User;
import com.devlog.repository.DevNoteRepository;
import com.devlog.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DevNoteService {

    private final DevNoteRepository devNoteRepository;
    private final UserRepository userRepository;

    public DevNoteService(DevNoteRepository devNoteRepository, UserRepository userRepository) {
        this.devNoteRepository = devNoteRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DevNote create(Long authorId, DevNoteCreateRequest request) {
        User author = userRepository.getReferenceById(authorId);
        DevNote devNote = DevNote.create(request.title(), request.content(), author);
        return devNoteRepository.save(devNote);
    }

    public DevNote findDetail(Long noteId, Long authorId) {
        return devNoteRepository.findByIdAndAuthor_Id(noteId, authorId)
                .orElseThrow(DevNoteNotFoundException::new);
    }

    public Page<DevNote> findByAuthor(Long authorId, Pageable pageable) {
        return devNoteRepository.findByAuthor_Id(authorId, pageable);
    }

    @Transactional
    public DevNote update(Long noteId, Long authorId, DevNoteUpdateRequest request) {
        DevNote devNote = devNoteRepository.findByIdAndAuthor_Id(noteId, authorId)
                .orElseThrow(DevNoteNotFoundException::new);
        devNote.update(request.title(), request.content());
        return devNoteRepository.saveAndFlush(devNote);
    }

    @Transactional
    public void delete(Long noteId, Long authorId) {
        DevNote devNote = devNoteRepository.findByIdAndAuthor_Id(noteId, authorId)
                .orElseThrow(DevNoteNotFoundException::new);
        devNoteRepository.delete(devNote);
    }
}
