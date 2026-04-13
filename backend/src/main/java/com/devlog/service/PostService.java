package com.devlog.service;

import com.devlog.common.exception.PostAccessDeniedException;
import com.devlog.common.exception.PostNotFoundException;
import com.devlog.controller.dto.PostCreateRequest;
import com.devlog.controller.dto.PostUpdateRequest;
import com.devlog.domain.Post;
import com.devlog.domain.User;
import com.devlog.repository.PostRepository;
import com.devlog.repository.UserRepository;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Post create(Long authorId, PostCreateRequest request) {
        User author = userRepository.getReferenceById(authorId);
        Post post = Post.create(
                request.title(),
                request.content(),
                author,
                normalizeTags(request.tags()));
        return postRepository.save(post);
    }

    @Transactional
    public Post update(Long postId, Long authorId, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        if (!post.isAuthoredBy(authorId)) {
            throw new PostAccessDeniedException();
        }
        post.update(
                request.title(),
                request.content(),
                normalizeTags(request.tags()));
        return postRepository.saveAndFlush(post);
    }

    @Transactional
    public void delete(Long postId, Long authorId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        if (!post.isAuthoredBy(authorId)) {
            throw new PostAccessDeniedException();
        }
        postRepository.delete(post);
    }

    public Post findDetail(Long postId) {
        return postRepository.findDetailById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    public Page<Post> findAll(Pageable pageable) {
        return postRepository.findAll(pageable);
    }

    public Page<Post> findByAuthor(Long authorId, Pageable pageable) {
        return postRepository.findByAuthor_Id(authorId, pageable);
    }

    public Page<Post> findByTag(String tag, Pageable pageable) {
        return postRepository.findByTag(tag, pageable);
    }

    private Set<String> normalizeTags(Set<String> tags) {
        return tags == null ? Set.of() : Set.copyOf(tags);
    }
}
