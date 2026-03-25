package com.hantu.post_service.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.hantu.post_service.entity.Reaction;

@Repository
public interface ReactionRepository extends MongoRepository<Reaction, String> {
    Optional<Reaction> findByUserIdAndTargetTypeAndTargetId(String userId, String targetType, String targetId);
    long countByTargetTypeAndTargetId(String targetType, String targetId);

    long countByTargetTypeAndTargetIdAndReactionType(String targetType, String targetId, String reactionType);
    void deleteByTargetTypeAndTargetId(String targetType, String targetId);
}
