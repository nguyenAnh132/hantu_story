package com.hantu.post_service.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.hantu.post_service.entity.Story;

@Repository
public interface StoryRepository extends MongoRepository<Story, String> {
    List<Story> findByAuthorIdOrderByCreatedAtDesc(String authorId);
    List<Story> findByAuthorIdAndStatusOrderByCreatedAtDesc(String authorId, String status);

    Page<Story> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
