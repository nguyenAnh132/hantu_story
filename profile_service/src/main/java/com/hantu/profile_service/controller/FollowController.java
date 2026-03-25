package com.hantu.profile_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hantu.profile_service.dto.response.FollowingPageResponse;
import com.hantu.profile_service.dto.response.FollowersPageResponse;
import com.hantu.profile_service.dto.response.ApiResponse;
import com.hantu.profile_service.service.FollowService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/follows")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FollowController {

    FollowService followService;

    @PostMapping("/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> follow(@PathVariable String targetUserId) {
        followService.follow(targetUserId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Followed successfully")
                .build());
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> unfollow(@PathVariable String targetUserId) {
        followService.unfollow(targetUserId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Unfollowed successfully")
                .build());
    }

    @GetMapping("/me/following")
    public ResponseEntity<ApiResponse<FollowingPageResponse>> getFollowing(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.<FollowingPageResponse>builder()
                .message("Following retrieved successfully")
                .result(followService.getFollowing(cursor, size))
                .build());
    }

    @GetMapping("/me/followers")
    public ResponseEntity<ApiResponse<FollowersPageResponse>> getFollowers(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.<FollowersPageResponse>builder()
                .message("Followers retrieved successfully")
                .result(followService.getFollowers(cursor, size))
                .build());
    }

    @GetMapping("/me/is-following/{targetUserId}")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(@PathVariable String targetUserId) {
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .message("Follow status retrieved successfully")
                .result(followService.isFollowing(targetUserId))
                .build());
    }

    @GetMapping("/me/following/count")
    public ResponseEntity<ApiResponse<Long>> countFollowing() {
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .message("Following count retrieved successfully")
                .result(followService.countFollowing())
                .build());
    }

    @GetMapping("/me/followers/count")
    public ResponseEntity<ApiResponse<Long>> countFollowers() {
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .message("Followers count retrieved successfully")
                .result(followService.countFollowers())
                .build());
    }
}

