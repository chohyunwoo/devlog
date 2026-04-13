package com.devlog.repository;

import com.devlog.domain.Post;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Override
    @EntityGraph(attributePaths = {"author"})
    Page<Post> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Post> findByAuthor_Id(Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query(
            value = "SELECT p FROM Post p WHERE :tag MEMBER OF p.tags",
            countQuery = "SELECT COUNT(p) FROM Post p WHERE :tag MEMBER OF p.tags"
    )
    Page<Post> findByTag(@Param("tag") String tag, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id = :id")
    Optional<Post> findDetailById(@Param("id") Long id);

    boolean existsByIdAndAuthor_Id(Long id, Long authorId);
}
