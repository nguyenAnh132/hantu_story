package com.hantu.post_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hantu.post_service.dto.request.CreateStoryRequest;
import com.hantu.post_service.dto.request.UpdateStoryRequest;
import com.hantu.post_service.dto.response.ApiResponse;
import com.hantu.post_service.dto.response.StoryPageResponse;
import com.hantu.post_service.dto.response.StoryResponse;
import com.hantu.post_service.service.StoryService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/stories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryController {
    StoryService storyService;

    @GetMapping
    public ResponseEntity<ApiResponse<StoryPageResponse>> getStories(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.<StoryPageResponse>builder()
                .message("Stories retrieved successfully")
                .result(storyService.getStories(cursor, size))
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StoryResponse>> createStory(@RequestBody CreateStoryRequest request) {
        return ResponseEntity.ok(ApiResponse.<StoryResponse>builder()
                .message("Story created successfully")
                .result(storyService.createStory(request))
                .build());
    }

    @GetMapping("/{storyId}")
    public ResponseEntity<ApiResponse<StoryResponse>> getStoryById(@PathVariable String storyId) {
        return ResponseEntity.ok(ApiResponse.<StoryResponse>builder()
                .message("Story retrieved successfully")
                .result(storyService.getStoryById(storyId))
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<StoryResponse>>> getMyStories() {
        return ResponseEntity.ok(ApiResponse.<List<StoryResponse>>builder()
                .message("Stories retrieved successfully")
                .result(storyService.getMyStories())
                .build());
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<ApiResponse<List<StoryResponse>>> getPublicStoriesByAuthor(@PathVariable String authorId) {
        return ResponseEntity.ok(ApiResponse.<List<StoryResponse>>builder()
                .message("Stories retrieved successfully")
                .result(storyService.getPublicStoriesByAuthor(authorId))
                .build());
    }

    @GetMapping("/following")
    public ResponseEntity<ApiResponse<StoryPageResponse>> getFollowingStories(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.<StoryPageResponse>builder()
                .message("Following stories retrieved successfully")
                .result(storyService.getFollowingStories(cursor, size))
                .build());
    }

    @PutMapping("/{storyId}")
    public ResponseEntity<ApiResponse<StoryResponse>> updateStory(@PathVariable String storyId,
            @RequestBody UpdateStoryRequest request) {
        return ResponseEntity.ok(ApiResponse.<StoryResponse>builder()
                .message("Story updated successfully")
                .result(storyService.updateStory(storyId, request))
                .build());
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<ApiResponse<Void>> deleteStory(@PathVariable String storyId) {
        storyService.deleteStory(storyId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Story deleted successfully")
                .build());
    }
}
