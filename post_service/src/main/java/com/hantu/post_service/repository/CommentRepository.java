package com.hantu.post_service.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.hantu.post_service.entity.Comment;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByStoryPartIdAndParentCommentIdIsNullOrderByCreatedAtDesc(String storyPartId);
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(String parentCommentId);
    void deleteByStoryPartId(String storyPartId);
}
