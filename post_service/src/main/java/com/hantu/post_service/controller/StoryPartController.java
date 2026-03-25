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
import org.springframework.web.bind.annotation.RestController;

import com.hantu.post_service.dto.request.CreateStoryPartRequest;
import com.hantu.post_service.dto.request.UpdateStoryPartRequest;
import com.hantu.post_service.dto.response.ApiResponse;
import com.hantu.post_service.dto.response.StoryPartResponse;
import com.hantu.post_service.service.StoryPartService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/story-parts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryPartController {
    StoryPartService storyPartService;

    @PostMapping
    public ResponseEntity<ApiResponse<StoryPartResponse>> createStoryPart(@RequestBody CreateStoryPartRequest request) {
        return ResponseEntity.ok(ApiResponse.<StoryPartResponse>builder()
                .message("Story part created successfully")
                .result(storyPartService.createStoryPart(request))
                .build());
    }

    @GetMapping("/{storyPartId}")
    public ResponseEntity<ApiResponse<StoryPartResponse>> getStoryPartById(@PathVariable String storyPartId) {
        return ResponseEntity.ok(ApiResponse.<StoryPartResponse>builder()
                .message("Story part retrieved successfully")
                .result(storyPartService.getStoryPartById(storyPartId))
                .build());
    }

    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<List<StoryPartResponse>>> getStoryPartsByStory(@PathVariable String storyId) {
        return ResponseEntity.ok(ApiResponse.<List<StoryPartResponse>>builder()
                .message("Story parts retrieved successfully")
                .result(storyPartService.getStoryPartsByStory(storyId))
                .build());
    }

    @PutMapping("/{storyPartId}")
    public ResponseEntity<ApiResponse<StoryPartResponse>> updateStoryPart(@PathVariable String storyPartId,
            @RequestBody UpdateStoryPartRequest request) {
        return ResponseEntity.ok(ApiResponse.<StoryPartResponse>builder()
                .message("Story part updated successfully")
                .result(storyPartService.updateStoryPart(storyPartId, request))
                .build());
    }

    @DeleteMapping("/{storyPartId}")
    public ResponseEntity<ApiResponse<Void>> deleteStoryPart(@PathVariable String storyPartId) {
        storyPartService.deleteStoryPart(storyPartId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Story part deleted successfully")
                .build());
    }
}
