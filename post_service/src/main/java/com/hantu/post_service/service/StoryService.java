package com.hantu.post_service.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Base64;
import java.util.LinkedHashSet;
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

import com.hantu.post_service.dto.request.CreateStoryRequest;
import com.hantu.post_service.dto.request.UpdateStoryRequest;
import com.hantu.post_service.dto.response.StoryPageResponse;
import com.hantu.post_service.dto.response.StoryResponse;
import com.hantu.post_service.dto.response.ApiResponse;
import com.hantu.post_service.dto.response.UserProfileResponse;
import com.hantu.post_service.entity.Story;
import com.hantu.post_service.exception.AppException;
import com.hantu.post_service.exception.ErrorCode;
import com.hantu.post_service.repository.StoryRepository;
import com.hantu.post_service.mapper.StoryMapper;
import com.hantu.post_service.StoryStatus;
import com.hantu.post_service.repository.httpclient.FollowClient;
import com.hantu.post_service.repository.httpclient.UserProfileClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import com.hantu.post_service.dto.response.FollowingPageResponse;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryService {
    StoryRepository storyRepository;
    StoryMapper storyMapper;
    MongoTemplate mongoTemplate;
    UserProfileClient userProfileClient;
    FollowClient followClient;

    int DEFAULT_PAGE_SIZE = 15;

    private static final int DEFAULT_CURSOR_SIZE = 10;
    private static final int MAX_CURSOR_SIZE = 50;
    private static final int MAX_FOLLOWING_FETCH_PER_FEED = 50;
    private static final int MAX_FOLLOWING_PAGES_PER_FEED = 5;

    public StoryPageResponse getStories(String cursor, int size) {
        int pageSize = normalizeSize(size);

        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(StoryStatus.PUBLISHED.name()));
        query.with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        query.limit(pageSize);

        Cursor cursorModel = Cursor.parse(cursor);
        if (cursorModel != null) {
            query.addCriteria(keysetAfter(cursorModel));
        }

        List<Story> entities = mongoTemplate.find(query, Story.class);
        List<StoryResponse> items = entities.stream().map(storyMapper::toStoryResponse).collect(Collectors.toList());

        enrichAuthorProfiles(items);
        return new StoryPageResponse(items, buildNextCursor(entities, pageSize));
    }

    public StoryPageResponse getFollowingStories(String cursor, int size) {
        int pageSize = normalizeSize(size);

        List<String> followingIds = fetchFollowingIdsForFeed();
        if (followingIds == null || followingIds.isEmpty()) {
            return new StoryPageResponse(List.of(), null);
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(StoryStatus.PUBLISHED.name()));
        query.addCriteria(Criteria.where("authorId").in(followingIds));
        query.with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        query.limit(pageSize);

        Cursor cursorModel = Cursor.parse(cursor);
        if (cursorModel != null) {
            query.addCriteria(keysetAfter(cursorModel));
        }

        List<Story> entities = mongoTemplate.find(query, Story.class);
        List<StoryResponse> items = entities.stream().map(storyMapper::toStoryResponse).collect(Collectors.toList());

        enrichAuthorProfiles(items);
        return new StoryPageResponse(items, buildNextCursor(entities, pageSize));
    }

    private List<String> fetchFollowingIdsForFeed() {
        Set<String> collectedIds = new LinkedHashSet<>();
        String cursor = null;
        int pageCount = 0;
        try {
            while (pageCount < MAX_FOLLOWING_PAGES_PER_FEED) {
                ApiResponse<FollowingPageResponse> response = followClient.getFollowing(
                        cursor,
                        MAX_FOLLOWING_FETCH_PER_FEED
                );
                if (response == null || response.getResult() == null) {
                    break;
                }

                FollowingPageResponse result = response.getResult();
                if (result.getFollowingIds() != null && !result.getFollowingIds().isEmpty()) {
                    collectedIds.addAll(result.getFollowingIds());
                }

                String nextCursor = normalizeCursor(result.getNextCursor());
                if (nextCursor == null) {
                    break;
                }
                cursor = nextCursor;
                pageCount++;
            }
            return collectedIds.stream().toList();
        } catch (Exception e) {
            log.warn("Failed to fetch following ids for feed", e);
            // Best-effort: if follow service is temporarily unavailable, keep UI responsive.
            return List.of();
        }
    }

    private String normalizeCursor(String cursor) {
        if (cursor == null) {
            return null;
        }
        String normalized = cursor.trim();
        if (normalized.isEmpty() || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private int normalizeSize(int size) {
        if (size <= 0) return DEFAULT_CURSOR_SIZE;
        return Math.min(size, MAX_CURSOR_SIZE);
    }

    private Criteria keysetAfter(Cursor cursorModel) {
        Criteria olderThanCreatedAt = Criteria.where("createdAt").lt(cursorModel.lastCreatedAt());
        Criteria sameCreatedAtOlderThanId = new Criteria().andOperator(
                Criteria.where("createdAt").is(cursorModel.lastCreatedAt()),
                Criteria.where("id").lt(cursorModel.lastId())
        );
        return new Criteria().orOperator(olderThanCreatedAt, sameCreatedAtOlderThanId);
    }

    private String buildNextCursor(List<Story> entities, int pageSize) {
        if (entities == null || entities.isEmpty()) return null;
        if (entities.size() < pageSize) return null; // Không còn trang nữa

        Story last = entities.get(entities.size() - 1);
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


    public StoryResponse createStory(CreateStoryRequest request) {
        Story story = new Story();
        var now = LocalDateTime.now();
        story.setTitle(request.getTitle() == null ? null : request.getTitle().trim());
        story.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        story.setId(null);
        story.setAuthorId(currentUserId());
        story.setCreatedAt(now);
        story.setUpdatedAt(now);
        if (request.getStatus() == null || request.getStatus().isBlank()) {
            story.setStatus(StoryStatus.PUBLISHED.name());
        } else {
            story.setStatus(request.getStatus().trim());
        }
        StoryResponse created = storyMapper.toStoryResponse(storyRepository.save(story));
        enrichAuthorProfile(created);
        return created;
    }

    public StoryResponse getStoryById(String storyId) {
        Story story = findStoryById(storyId);
        if (!StoryStatus.PUBLISHED.name().equals(story.getStatus())) {
            // Hide non-public stories from non-owners (helps UI avoid rendering drafts).
            String currentUserId = currentUserIdOrNull();
            if (currentUserId == null || !currentUserId.equals(story.getAuthorId())) {
                throw new AppException(ErrorCode.STORY_NOT_FOUND);
            }
        }

        StoryResponse response = storyMapper.toStoryResponse(story);
        enrichAuthorProfile(response);
        return response;
    }

    public List<StoryResponse> getMyStories() {
        List<StoryResponse> items = storyRepository.findByAuthorIdOrderByCreatedAtDesc(currentUserId()).stream()
                .map(storyMapper::toStoryResponse)
                .collect(Collectors.toList());
        enrichAuthorProfiles(items);
        return items;
    }

    public List<StoryResponse> getPublicStoriesByAuthor(String authorId) {
        String normalizedAuthorId = authorId == null ? "" : authorId.trim();
        if (normalizedAuthorId.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        List<StoryResponse> items = storyRepository
                .findByAuthorIdAndStatusOrderByCreatedAtDesc(normalizedAuthorId, StoryStatus.PUBLISHED.name())
                .stream()
                .map(storyMapper::toStoryResponse)
                .collect(Collectors.toList());
        enrichAuthorProfiles(items);
        return items;
    }

    public StoryResponse updateStory(String storyId, UpdateStoryRequest request) {
        Story story = findStoryById(storyId);
        validateOwner(story);

        storyMapper.updateStory(request, story);
        story.setUpdatedAt(LocalDateTime.now());
        StoryResponse updated = storyMapper.toStoryResponse(storyRepository.save(story));
        enrichAuthorProfile(updated);
        return updated;
    }

    public void deleteStory(String storyId) {
        Story story = findStoryById(storyId);
        validateOwner(story);
        storyRepository.delete(story);
    }

    private Story findStoryById(String storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
    }

    private void validateOwner(Story story) {
        if (!currentUserId().equals(story.getAuthorId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getName();
    }

    public Page<StoryResponse> getStories(int page) {
        Pageable pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE);
        Page<Story> stories = storyRepository.findByStatusOrderByCreatedAtDesc(StoryStatus.PUBLISHED.name(), pageable);
        Page<StoryResponse> result = stories.map(storyMapper::toStoryResponse);
        enrichAuthorProfiles(result.getContent());
        return result;
    }

    private void enrichAuthorProfiles(List<StoryResponse> stories) {
        if (stories == null || stories.isEmpty()) return;

        Set<String> authorIds = stories.stream()
                .map(StoryResponse::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, UserProfileResponse> profilesByUserId = new HashMap<>();
        for (String authorId : authorIds) {
            UserProfileResponse profile = fetchUserProfileOrNull(authorId);
            if (profile != null) {
                profilesByUserId.put(authorId, profile);
            }
        }

        for (StoryResponse story : stories) {
            UserProfileResponse profile = profilesByUserId.get(story.getAuthorId());
            if (profile == null) continue;

            story.setAuthorProfileId(profile.getId());
            story.setAuthorFirstName(profile.getFirstName());
            story.setAuthorLastName(profile.getLastName());
            story.setAuthorProfilePicture(profile.getProfilePicture());
        }
    }

    private void enrichAuthorProfile(StoryResponse story) {
        if (story == null || story.getAuthorId() == null) return;
        UserProfileResponse profile = fetchUserProfileOrNull(story.getAuthorId());
        if (profile == null) return;

        story.setAuthorProfileId(profile.getId());
        story.setAuthorFirstName(profile.getFirstName());
        story.setAuthorLastName(profile.getLastName());
        story.setAuthorProfilePicture(profile.getProfilePicture());
    }

    private UserProfileResponse fetchUserProfileOrNull(String userId) {
        try {
            ResponseEntity<ApiResponse<UserProfileResponse>> response = userProfileClient.getUserProfile(userId);
            if (response == null || response.getBody() == null || response.getBody().getResult() == null) {
                return null;
            }
            return response.getBody().getResult();
        } catch (Exception e) {
            // Best-effort enrichment: missing/corrupt profile should not block story feed UI.
            return null;
        }
    }

    private String currentUserIdOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName();
    }
}
