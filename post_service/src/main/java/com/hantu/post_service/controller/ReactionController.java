package com.hantu.post_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hantu.post_service.dto.request.UpsertReactionRequest;
import com.hantu.post_service.dto.response.ApiResponse;
import com.hantu.post_service.dto.response.ReactionResponse;
import com.hantu.post_service.service.ReactionService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/reactions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReactionController {
    ReactionService reactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReactionResponse>> upsertReaction(@RequestBody UpsertReactionRequest request) {
        return ResponseEntity.ok(ApiResponse.<ReactionResponse>builder()
                .message("Reaction updated successfully")
                .result(reactionService.upsertReaction(request))
                .build());
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteReaction(@RequestParam String targetType,
            @RequestParam String targetId) {
        reactionService.deleteReaction(targetType, targetId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Reaction deleted successfully")
                .build());
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countReactions(@RequestParam String targetType,
            @RequestParam String targetId,
            @RequestParam(required = false) String reactionType) {
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .message("Reaction count retrieved successfully")
                .result(reactionService.countReactions(targetType, targetId, reactionType))
                .build());
    }

    // Used by UI to toggle like/dislike precisely for the currently logged-in user.
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ReactionResponse>> getMyReaction(
            @RequestParam String targetType,
            @RequestParam String targetId) {
        return ResponseEntity.ok(ApiResponse.<ReactionResponse>builder()
                .message("Your reaction retrieved successfully")
                .result(reactionService.getMyReaction(targetType, targetId))
                .build());
    }
}
