package com.hantu.post_service.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.hantu.post_service.dto.request.CreateCommentRequest;
import com.hantu.post_service.dto.response.ApiResponse;
import com.hantu.post_service.dto.response.CommentResponse;
import com.hantu.post_service.dto.response.CommentPageResponse;
import com.hantu.post_service.dto.response.UserProfileResponse;
import com.hantu.post_service.mapper.CommentMapper;
import com.hantu.post_service.entity.Comment;
import com.hantu.post_service.entity.Story;
import com.hantu.post_service.entity.StoryPart;
import com.hantu.post_service.StoryStatus;
import com.hantu.post_service.exception.AppException;
import com.hantu.post_service.exception.ErrorCode;
import com.hantu.post_service.repository.CommentRepository;
import com.hantu.post_service.repository.httpclient.UserProfileClient;
import com.hantu.post_service.repository.StoryRepository;
import com.hantu.post_service.repository.StoryPartRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    /** Giới hạn một lần tải toàn bộ comment theo story (tránh payload quá lớn). */
    private static final int MAX_COMMENTS_PER_STORY = 500;

    MongoTemplate mongoTemplate;
    CommentRepository commentRepository;
    StoryPartRepository storyPartRepository;
    StoryRepository storyRepository;
    CommentMapper commentMapper;
    UserProfileClient userProfileClient;

    public CommentPageResponse getTopLevelComments(String storyPartId, String cursor, int size) {
        int pageSize = normalizeSize(size);

        validateStoryPartAccessible(storyPartId);

        Cursor cursorModel = Cursor.parse(cursor);
        Criteria baseCriteria = new Criteria().andOperator(
                Criteria.where("storyPartId").is(storyPartId),
                Criteria.where("parentCommentId").is(null),
                notDeletedCriteria()
        );
        Query query = new Query();
        if (cursorModel == null) {
            query.addCriteria(baseCriteria);
        } else {
            query.addCriteria(new Criteria().andOperator(baseCriteria, keysetAfterTopLevel(cursorModel)));
        }
        query.with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        query.limit(pageSize);

        List<Comment> entities = mongoTemplate.find(query, Comment.class);
        List<CommentResponse> items = entities.stream().map(commentMapper::toCommentResponse).collect(Collectors.toList());
        enrichAuthorProfiles(items);

        return new CommentPageResponse(items, buildNextCursor(entities, pageSize));
    }

    public CommentPageResponse getReplies(String parentCommentId, String cursor, int size) {
        int pageSize = normalizeSize(size);
        Comment parent = findCommentById(parentCommentId);
        validateCommentAccessible(parent);

        Cursor cursorModel = Cursor.parse(cursor);
        Criteria baseCriteria = new Criteria().andOperator(
                Criteria.where("parentCommentId").is(parentCommentId),
                notDeletedCriteria()
        );
        Query query = new Query();
        if (cursorModel == null) {
            query.addCriteria(baseCriteria);
        } else {
            query.addCriteria(new Criteria().andOperator(baseCriteria, keysetAfterReply(cursorModel)));
        }
        query.with(Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        query.limit(pageSize);

        List<Comment> entities = mongoTemplate.find(query, Comment.class);
        List<CommentResponse> items = entities.stream().map(commentMapper::toCommentResponse).collect(Collectors.toList());
        enrichAuthorProfiles(items);

        return new CommentPageResponse(items, buildNextCursor(entities, pageSize));
    }

    public CommentResponse createComment(CreateCommentRequest request) {
        String requestStoryId = normalizeBlank(request.getStoryId());
        String requestStoryPartId = normalizeBlank(request.getStoryPartId());
        String targetStoryId;
        String targetStoryPartId;

        if (requestStoryPartId != null) {
            StoryPart part = findAccessibleStoryPartOrThrow(requestStoryPartId);
            targetStoryPartId = part.getId();
            targetStoryId = part.getStoryId();
        } else if (requestStoryId != null) {
            Story story = findAccessibleStoryOrThrow(requestStoryId);
            targetStoryId = story.getId();
            targetStoryPartId = null;
        } else {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        var now = LocalDateTime.now();
        Comment comment = commentMapper.toComment(request);
        comment.setId(null);
        comment.setAuthorId(currentUserId());
        comment.setStoryId(targetStoryId);
        comment.setStoryPartId(targetStoryPartId);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        comment.setDeleted(false);
        comment.setLikeCount(0);
        comment.setReplyCount(0);

        if (comment.getParentCommentId() == null || comment.getParentCommentId().isBlank()) {
            // Normalize empty-string to null so keyset pagination queries match `parentCommentId is null`.
            comment.setParentCommentId(null);
            comment.setRootCommentId(null);
            comment.setDepth(0);
            CommentResponse created = commentMapper.toCommentResponse(commentRepository.save(comment));
            enrichAuthorProfile(created);
            return created;
        }

        Comment parent = commentRepository.findById(comment.getParentCommentId())
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (parent.isDeleted()) {
            throw new AppException(ErrorCode.COMMENT_NOT_FOUND);
        }
        // Keep reply thread consistent regardless of what client sends in request.
        comment.setStoryId(parent.getStoryId());
        comment.setStoryPartId(parent.getStoryPartId());
        comment.setRootCommentId(parent.getRootCommentId() == null ? parent.getId() : parent.getRootCommentId());
        comment.setDepth(parent.getDepth() + 1);

        Comment saved = commentRepository.save(comment);
        parent.setReplyCount(parent.getReplyCount() + 1);
        commentRepository.save(parent);
        CommentResponse created = commentMapper.toCommentResponse(saved);
        enrichAuthorProfile(created);
        return created;
    }

    public CommentResponse getCommentById(String commentId) {
        Comment comment = findCommentById(commentId);
        validateCommentAccessible(comment);
        CommentResponse response = commentMapper.toCommentResponse(comment);
        enrichAuthorProfile(response);
        return response;
    }

    public void deleteComment(String commentId) {
        Comment comment = findCommentById(commentId);
        validateCommentAccessible(comment);
        if (!currentUserId().equals(comment.getAuthorId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        comment.setDeleted(true);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    private Comment findCommentById(String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (comment.isDeleted()) {
            throw new AppException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private int normalizeSize(int size) {
        if (size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Criteria keysetAfterTopLevel(Cursor cursorModel) {
        Criteria olderThanCreatedAt = Criteria.where("createdAt").lt(cursorModel.lastCreatedAt());
        Criteria sameCreatedAtOlderThanId = new Criteria().andOperator(
                Criteria.where("createdAt").is(cursorModel.lastCreatedAt()),
                Criteria.where("id").lt(cursorModel.lastId())
        );
        return new Criteria().orOperator(olderThanCreatedAt, sameCreatedAtOlderThanId);
    }

    private Criteria keysetAfterReply(Cursor cursorModel) {
        Criteria newerThanCreatedAt = Criteria.where("createdAt").gt(cursorModel.lastCreatedAt());
        Criteria sameCreatedAtNewerThanId = new Criteria().andOperator(
                Criteria.where("createdAt").is(cursorModel.lastCreatedAt()),
                Criteria.where("id").gt(cursorModel.lastId())
        );
        return new Criteria().orOperator(newerThanCreatedAt, sameCreatedAtNewerThanId);
    }

    private String buildNextCursor(List<Comment> entities, int pageSize) {
        if (entities == null || entities.isEmpty()) return null;
        if (entities.size() < pageSize) return null;

        Comment last = entities.get(entities.size() - 1);
        if (last.getCreatedAt() == null || last.getId() == null) return null;
        return Cursor.of(last.getCreatedAt(), last.getId()).encode();
    }

    private record Cursor(LocalDateTime lastCreatedAt, String lastId) {
        static Cursor parse(String cursor) {
            if (cursor == null || cursor.isBlank()) return null;
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(cursor));
                String[] parts = decoded.split("\\|", -1);
                long millis = Long.parseLong(parts[0]);
                String id = parts[1];
                LocalDateTime createdAt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault());
                return new Cursor(createdAt, id);
            } catch (Exception e) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
        }

        static Cursor of(LocalDateTime createdAt, String id) {
            return new Cursor(createdAt, id);
        }

        String encode() {
            long millis = createdAtToMillis(lastCreatedAt);
            String payload = millis + "|" + lastId;
            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        }

        private static long createdAtToMillis(LocalDateTime createdAt) {
            return createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
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

    public long countCommentsForStory(String storyId) {
        Story story = findAccessibleStoryOrThrow(storyId);
        List<StoryPart> parts = storyPartRepository.findByStoryIdOrderByOrderAsc(story.getId());
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                notDeletedCriteria(),
                storyCommentsCriteria(storyId, parts.stream().map(StoryPart::getId).toList())
        ));
        return mongoTemplate.count(query, Comment.class);
    }

    public List<CommentResponse> listCommentsForStory(String storyId) {
        findAccessibleStoryOrThrow(storyId);
        List<StoryPart> parts = storyPartRepository.findByStoryIdOrderByOrderAsc(storyId);
        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                notDeletedCriteria(),
                storyCommentsCriteria(storyId, parts.stream().map(StoryPart::getId).toList())
        ));
        query.with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        query.limit(MAX_COMMENTS_PER_STORY);

        List<Comment> entities = mongoTemplate.find(query, Comment.class);
        List<CommentResponse> items = entities.stream()
                .map(commentMapper::toCommentResponse)
                .collect(Collectors.toList());
        enrichAuthorProfiles(items);
        return items;
    }

    private Story findAccessibleStoryOrThrow(String storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
        if (StoryStatus.PUBLISHED.name().equals(story.getStatus())) {
            return story;
        }
        String uid = currentUserIdOrNull();
        if (uid == null || !uid.equals(story.getAuthorId())) {
            throw new AppException(ErrorCode.STORY_NOT_FOUND);
        }
        return story;
    }

    private void validateStoryPartAccessible(String storyPartId) {
        findAccessibleStoryPartOrThrow(storyPartId);
    }

    private StoryPart findAccessibleStoryPartOrThrow(String storyPartId) {
        StoryPart part = storyPartRepository.findById(storyPartId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_PART_NOT_FOUND));
        findAccessibleStoryOrThrow(part.getStoryId());
        return part;
    }

    private void validateCommentAccessible(Comment comment) {
        if (comment.getStoryPartId() != null && !comment.getStoryPartId().isBlank()) {
            validateStoryPartAccessible(comment.getStoryPartId());
            return;
        }
        String storyId = normalizeBlank(comment.getStoryId());
        if (storyId == null) {
            throw new AppException(ErrorCode.COMMENT_NOT_FOUND);
        }
        findAccessibleStoryOrThrow(storyId);
    }

    private Criteria storyCommentsCriteria(String storyId, List<String> partIds) {
        if (partIds == null || partIds.isEmpty()) {
            return Criteria.where("storyId").is(storyId);
        }
        // Backward compatibility: old comments may only have storyPartId.
        return new Criteria().orOperator(
                Criteria.where("storyId").is(storyId),
                Criteria.where("storyPartId").in(partIds)
        );
    }

    private Criteria notDeletedCriteria() {
        // Backward compatibility across different persisted field names and old docs.
        return new Criteria().orOperator(
                Criteria.where("deleted").is(false),
                Criteria.where("isDeleted").is(false),
                new Criteria().andOperator(
                        Criteria.where("deleted").exists(false),
                        Criteria.where("isDeleted").exists(false)
                )
        );
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void enrichAuthorProfiles(List<CommentResponse> comments) {
        if (comments == null || comments.isEmpty()) return;

        Set<String> authorIds = comments.stream()
                .map(CommentResponse::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, UserProfileResponse> profilesByUserId = new HashMap<>();
        for (String authorId : authorIds) {
            UserProfileResponse profile = fetchUserProfileOrNull(authorId);
            if (profile != null) {
                profilesByUserId.put(authorId, profile);
            }
        }

        for (CommentResponse comment : comments) {
            UserProfileResponse profile = profilesByUserId.get(comment.getAuthorId());
            if (profile == null) continue;

            comment.setAuthorProfileId(profile.getId());
            comment.setAuthorFirstName(profile.getFirstName());
            comment.setAuthorLastName(profile.getLastName());
            comment.setAuthorProfilePicture(profile.getProfilePicture());
        }
    }

    private void enrichAuthorProfile(CommentResponse comment) {
        if (comment == null || comment.getAuthorId() == null) return;
        UserProfileResponse profile = fetchUserProfileOrNull(comment.getAuthorId());
        if (profile == null) return;

        comment.setAuthorProfileId(profile.getId());
        comment.setAuthorFirstName(profile.getFirstName());
        comment.setAuthorLastName(profile.getLastName());
        comment.setAuthorProfilePicture(profile.getProfilePicture());
    }

    private UserProfileResponse fetchUserProfileOrNull(String userId) {
        try {
            ResponseEntity<ApiResponse<UserProfileResponse>> response = userProfileClient.getUserProfile(userId);
            if (response == null || response.getBody() == null || response.getBody().getResult() == null) {
                return null;
            }
            return response.getBody().getResult();
        } catch (Exception e) {
            // Best-effort enrichment: missing/corrupt profile should not break the whole comment feed.
            return null;
        }
    }
}
