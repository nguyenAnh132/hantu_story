package com.hantu.post_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.hantu.post_service.dto.request.CreateCommentRequest;
import com.hantu.post_service.dto.response.CommentPageResponse;
import com.hantu.post_service.dto.response.ApiResponse;
import com.hantu.post_service.dto.response.CommentResponse;
import com.hantu.post_service.service.CommentService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentController {
    CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(@RequestBody CreateCommentRequest request) {
        return ResponseEntity.ok(ApiResponse.<CommentResponse>builder()
                .message("Comment created successfully")
                .result(commentService.createComment(request))
                .build());
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> getCommentById(@PathVariable String commentId) {
        return ResponseEntity.ok(ApiResponse.<CommentResponse>builder()
                .message("Comment retrieved successfully")
                .result(commentService.getCommentById(commentId))
                .build());
    }

    @GetMapping("/story-part/{storyPartId}")
    public ResponseEntity<ApiResponse<CommentPageResponse>> getTopLevelComments(
            @PathVariable String storyPartId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.<CommentPageResponse>builder()
                .message("Comments retrieved successfully")
                .result(commentService.getTopLevelComments(storyPartId, cursor, size))
                .build());
    }

    @GetMapping("/story/{storyId}/count")
    public ResponseEntity<ApiResponse<Long>> countCommentsForStory(@PathVariable String storyId) {
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .message("Comment count retrieved successfully")
                .result(commentService.countCommentsForStory(storyId))
                .build());
    }

    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> listCommentsForStory(
            @PathVariable String storyId) {
        return ResponseEntity.ok(ApiResponse.<List<CommentResponse>>builder()
                .message("Comments retrieved successfully")
                .result(commentService.listCommentsForStory(storyId))
                .build());
    }

    @GetMapping("/{parentCommentId}/replies")
    public ResponseEntity<ApiResponse<CommentPageResponse>> getReplies(
            @PathVariable String parentCommentId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.<CommentPageResponse>builder()
                .message("Replies retrieved successfully")
                .result(commentService.getReplies(parentCommentId, cursor, size))
                .build());
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable String commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Comment deleted successfully")
                .build());
    }
}
