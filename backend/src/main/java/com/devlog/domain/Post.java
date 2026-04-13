package com.devlog.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * equals/hashCode는 정의하지 않습니다.
 * 참조 동등성에 의존하므로 {@code Set<Post>}나 detach/merge 혼합 컬렉션에 담지 마세요.
 */
@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_posts_author_created", columnList = "author_id, created_at"),
                @Index(name = "idx_posts_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Post {

    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_TAG_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private User author;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id")
    )
    @Column(name = "tag", length = MAX_TAG_LENGTH, nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Post(String title, String content, User author, Set<String> tags) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.tags = new LinkedHashSet<>(tags);
    }

    public static Post create(String title, String content, User author, Set<String> tags) {
        Objects.requireNonNull(tags, "tags must not be null; use Set.of() for no tags");
        return new Post(title, content, author, tags);
    }

    public void update(String title, String content, Set<String> tags) {
        Objects.requireNonNull(tags, "tags must not be null; use Set.of() to clear");
        this.title = title;
        this.content = content;
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public boolean isAuthoredBy(Long userId) {
        return this.author.getId().equals(userId);
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }
}
