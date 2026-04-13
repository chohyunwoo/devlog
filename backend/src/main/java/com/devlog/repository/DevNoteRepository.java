package com.devlog.repository;

import com.devlog.domain.DevNote;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * DevNote 는 비공개(작성자 본인만 접근) 엔티티입니다.
 * 모든 조회 메서드가 author 필터를 강제하므로,
 * 이 리포지토리를 사용하는 쪽은 상속된 {@code findById} / {@code findAll}을 호출하지 않도록 주의하세요.
 */
public interface DevNoteRepository extends JpaRepository<DevNote, Long> {

    Page<DevNote> findByAuthor_Id(Long authorId, Pageable pageable);

    Optional<DevNote> findByIdAndAuthor_Id(Long id, Long authorId);
}
