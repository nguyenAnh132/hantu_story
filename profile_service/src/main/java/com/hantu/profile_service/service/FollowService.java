package com.hantu.profile_service.service;

import java.util.Base64;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.hantu.profile_service.dto.response.FollowingPageResponse;
import com.hantu.profile_service.dto.response.FollowersPageResponse;
import com.hantu.profile_service.exception.AppException;
import com.hantu.profile_service.exception.ErrorCode;
import com.hantu.profile_service.repository.UserProfileRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FollowService {

    UserProfileRepository userProfileRepository;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    public void follow(String targetUserId) {
        String followerUserId = currentUserId();
        if (followerUserId.equals(targetUserId)) {
            throw new AppException(ErrorCode.FOLLOWING_SELF);
        }

        // Validate target existence (avoid creating dangling relationship).
        userProfileRepository.findByUserId(followerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
        userProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));

        userProfileRepository.follow(followerUserId, targetUserId);
    }

    public void unfollow(String targetUserId) {
        String followerUserId = currentUserId();
        if (followerUserId.equals(targetUserId)) {
            throw new AppException(ErrorCode.FOLLOWING_SELF);
        }

        userProfileRepository.findByUserId(followerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_PROFILE_NOT_FOUND));
        // If target doesn't exist, treat it as no-op to keep UX stable.
        // (You can tighten this later if product requires strict validation.)
        userProfileRepository.unfollow(followerUserId, targetUserId);
    }

    public FollowingPageResponse getFollowing(String cursor, int size) {
        int pageSize = normalizeSize(size);
        String followerUserId = currentUserId();

        Cursor cursorModel = Cursor.parse(cursor);
        List<UserProfileRepository.FollowingEdge> edges = cursorModel == null
                ? userProfileRepository.findFollowingEdgesFirstPage(followerUserId, pageSize)
                : userProfileRepository.findFollowingEdgesAfterCursor(
                        followerUserId,
                        cursorModel.lastFollowedAtMillis(),
                        cursorModel.lastTargetUserId(),
                        pageSize
                );

        List<String> followingIds = edges.stream()
                .map(UserProfileRepository.FollowingEdge::getUserId)
                .toList();

        String nextCursor = null;
        if (edges.size() == pageSize) {
            var last = edges.get(edges.size() - 1);
            nextCursor = Cursor.of(last.getFollowedAtMillis(), last.getUserId()).encode();
        }

        return FollowingPageResponse.builder()
                .followingIds(followingIds)
                .nextCursor(nextCursor)
                .build();
    }

    public boolean isFollowing(String targetUserId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_TARGET_USER_ID);
        }

        String followerUserId = currentUserId();
        if (followerUserId.equals(targetUserId)) {
            // Following yourself is not meaningful in UI; keep it stable.
            return false;
        }

        Long count = userProfileRepository.countFollowing(followerUserId, targetUserId);
        return count != null && count > 0;
    }

    public FollowersPageResponse getFollowers(String cursor, int size) {
        int pageSize = normalizeSize(size);
        String targetUserId = currentUserId();

        Cursor cursorModel = Cursor.parse(cursor);
        List<UserProfileRepository.FollowerEdge> edges = cursorModel == null
                ? userProfileRepository.findFollowerEdgesFirstPage(targetUserId, pageSize)
                : userProfileRepository.findFollowerEdgesAfterCursor(
                        targetUserId,
                        cursorModel.lastFollowedAtMillis(),
                        cursorModel.lastTargetUserId(),
                        pageSize
                );

        List<String> followerIds = edges.stream()
                .map(UserProfileRepository.FollowerEdge::getUserId)
                .toList();

        String nextCursor = null;
        if (edges.size() == pageSize) {
            var last = edges.get(edges.size() - 1);
            nextCursor = Cursor.of(last.getFollowedAtMillis(), last.getUserId()).encode();
        }

        return FollowersPageResponse.builder()
                .followerIds(followerIds)
                .nextCursor(nextCursor)
                .build();
    }

    public long countFollowing() {
        String followerUserId = currentUserId();
        Long count = userProfileRepository.countFollowingByFollower(followerUserId);
        return count == null ? 0 : count;
    }

    public long countFollowers() {
        String targetUserId = currentUserId();
        Long count = userProfileRepository.countFollowersByTarget(targetUserId);
        return count == null ? 0 : count;
    }

    private int normalizeSize(int size) {
        if (size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getName();
    }

    private record Cursor(long lastFollowedAtMillis, String lastTargetUserId) {
        static Cursor parse(String cursor) {
            if (cursor == null || cursor.isBlank()) return null;
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(cursor));
                String[] parts = decoded.split("\\|", -1);
                long millis = Long.parseLong(parts[0]);
                String userId = parts[1];
                return new Cursor(millis, userId);
            } catch (Exception e) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
        }

        static Cursor of(long followedAtMillis, String targetUserId) {
            return new Cursor(followedAtMillis, targetUserId);
        }

        String encode() {
            String payload = lastFollowedAtMillis + "|" + lastTargetUserId;
            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        }
    }
}

