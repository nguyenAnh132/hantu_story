package com.hantu.post_service.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.hantu.post_service.dto.request.UpsertReactionRequest;
import com.hantu.post_service.dto.response.ReactionResponse;
import com.hantu.post_service.mapper.ReactionMapper;
import com.hantu.post_service.StoryStatus;
import com.hantu.post_service.entity.Reaction;
import com.hantu.post_service.entity.Comment;
import com.hantu.post_service.entity.Story;
import com.hantu.post_service.entity.StoryPart;
import com.hantu.post_service.exception.AppException;
import com.hantu.post_service.exception.ErrorCode;
import com.hantu.post_service.repository.CommentRepository;
import com.hantu.post_service.repository.ReactionRepository;
import com.hantu.post_service.repository.StoryPartRepository;
import com.hantu.post_service.repository.StoryRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReactionService {
    private static final Set<String> SUPPORTED_TARGET_TYPES = Set.of("STORY", "STORY_PART", "COMMENT");

    ReactionRepository reactionRepository;
    StoryRepository storyRepository;
    StoryPartRepository storyPartRepository;
    CommentRepository commentRepository;
    ReactionMapper reactionMapper;

    public ReactionResponse upsertReaction(UpsertReactionRequest request) {
        String normalizedTargetType = normalizeTargetType(request.getTargetType());
        validateTargetAccessible(normalizedTargetType, request.getTargetId());
        var now = LocalDateTime.now();
        String userId = currentUserId();

        var existingReaction = reactionRepository.findByUserIdAndTargetTypeAndTargetId(userId, normalizedTargetType,
                request.getTargetId());
        if (existingReaction.isPresent()) {
            Reaction reaction = existingReaction.get();
            reaction.setReactionType(request.getReactionType());
            reaction.setUpdatedAt(now);
            return reactionMapper.toReactionResponse(reactionRepository.save(reaction));
        }

        Reaction reaction = new Reaction();
        reaction.setUserId(userId);
        reaction.setTargetType(normalizedTargetType);
        reaction.setTargetId(request.getTargetId());
        reaction.setReactionType(request.getReactionType());
        reaction.setCreatedAt(now);
        reaction.setUpdatedAt(now);
        return reactionMapper.toReactionResponse(reactionRepository.save(reaction));
    }

    public void deleteReaction(String targetType, String targetId) {
        String normalizedTargetType = normalizeTargetType(targetType);
        validateTargetAccessible(normalizedTargetType, targetId);
        String userId = currentUserId();
        reactionRepository.findByUserIdAndTargetTypeAndTargetId(userId, normalizedTargetType, targetId)
                .ifPresent(reactionRepository::delete);
    }

    public long countReactions(String targetType, String targetId) {
        return countReactions(targetType, targetId, null);
    }


    public long countReactions(String targetType, String targetId, String reactionType) {
        String normalizedTargetType = normalizeTargetType(targetType);
        validateTargetAccessible(normalizedTargetType, targetId);
        if (reactionType == null || reactionType.isBlank()) {
            return reactionRepository.countByTargetTypeAndTargetId(normalizedTargetType, targetId);
        }
        String normalizedReactionType = reactionType.trim().toUpperCase();
        return reactionRepository.countByTargetTypeAndTargetIdAndReactionType(
                normalizedTargetType, targetId, normalizedReactionType);
    }

    public ReactionResponse getMyReaction(String targetType, String targetId) {
        String normalizedTargetType = normalizeTargetType(targetType);
        validateTargetAccessible(normalizedTargetType, targetId);

        String userId = currentUserId();
        return reactionRepository.findByUserIdAndTargetTypeAndTargetId(userId, normalizedTargetType, targetId)
                .map(reactionMapper::toReactionResponse)
                .orElse(null);
    }

    private String normalizeTargetType(String targetType) {
        String normalized = targetType == null ? "" : targetType.trim().toUpperCase();
        if (!SUPPORTED_TARGET_TYPES.contains(normalized)) {
            throw new AppException(ErrorCode.INVALID_TARGET_TYPE);
        }
        return normalized;
    }

    private void validateTargetAccessible(String targetType, String targetId) {
        switch (targetType) {
            case "STORY" -> {
                Story story = storyRepository.findById(targetId)
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
                validateStoryAccessible(story);
            }
            case "STORY_PART" -> {
                StoryPart part = storyPartRepository.findById(targetId)
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
                Story story = storyRepository.findById(part.getStoryId())
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
                validateStoryAccessible(story);
            }
            case "COMMENT" -> {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
                if (comment.isDeleted()) {
                    throw new AppException(ErrorCode.COMMENT_NOT_FOUND);
                }
                StoryPart part = storyPartRepository.findById(comment.getStoryPartId())
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
                Story story = storyRepository.findById(part.getStoryId())
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
                validateStoryAccessible(story);
            }
            default -> throw new AppException(ErrorCode.INVALID_TARGET_TYPE);
        }
    }

    private String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getName();
    }

    private String currentUserIdOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName();
    }

    private void validateStoryAccessible(Story story) {
        if (StoryStatus.PUBLISHED.name().equals(story.getStatus())) {
            return;
        }
        String currentUserId = currentUserIdOrNull();
        if (currentUserId == null || !currentUserId.equals(story.getAuthorId())) {
            throw new AppException(ErrorCode.STORY_NOT_FOUND);
        }
    }
}
