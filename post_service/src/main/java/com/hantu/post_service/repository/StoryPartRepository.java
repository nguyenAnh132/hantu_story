package com.hantu.post_service.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.hantu.post_service.entity.StoryPart;

@Repository
public interface StoryPartRepository extends MongoRepository<StoryPart, String> {
    List<StoryPart> findByStoryIdOrderByOrderAsc(String storyId);
    boolean existsByStoryIdAndOrder(String storyId, int order);
    void deleteByStoryId(String storyId);
}
